package com.example.whattowatchnow.service;

import com.example.whattowatchnow.domain.Review;
import com.example.whattowatchnow.domain.User;
import com.example.whattowatchnow.repository.ReviewRepository;
import com.example.whattowatchnow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User testUser;
    private Review review;
    private String movieId = "movieId123";

    @BeforeEach
    void setUp() {
        testUser = new User("testUser", "test@example.com", "password", "pic.jpg");
        testUser.setId("userId123");

        review = new Review(testUser.getId(), movieId, 5, "Great movie!");
        review.setId("reviewId123");
        review.setReviewDate(LocalDateTime.now().minusDays(1)); // Set a fixed past date for predictability
        
        // Mock SecurityContext for authenticated user
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(testUser.getEmail());
        SecurityContextHolder.setContext(securityContext);
        
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
    }

    @Test
    void addReview_success() {
        Review newReview = new Review(null, null, 4, "Awesome!"); // UserId and MovieId will be set by service
        
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review savedReview = invocation.getArgument(0);
            savedReview.setId("newReviewId"); // Simulate ID generation on save
            return savedReview;
        });

        Review addedReview = reviewService.addReview(newReview, movieId);

        assertNotNull(addedReview);
        assertEquals(testUser.getId(), addedReview.getUserId());
        assertEquals(movieId, addedReview.getMovieId());
        assertEquals(4, addedReview.getRating());
        assertEquals("Awesome!", addedReview.getComment());
        assertNotNull(addedReview.getReviewDate());
        assertTrue(addedReview.getReviewDate().isAfter(LocalDateTime.now().minusMinutes(1))); // Ensure date is recent
        verify(reviewRepository, times(1)).save(any(Review.class));
    }
    
    @Test
    void addReview_userNotFoundInRepo_throwsRuntimeException() {
        // Override the default mock for userRepository for this specific test
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.empty());
        
        Review newReview = new Review(null, null, 4, "This should fail");
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reviewService.addReview(newReview, movieId);
        });
        
        assertEquals("User not found, cannot add review.", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }


    @Test
    void getReviewsForMovie_success() {
        Review review2 = new Review(testUser.getId(), movieId, 3, "Okay movie.");
        when(reviewRepository.findByMovieId(movieId)).thenReturn(Arrays.asList(review, review2));

        List<Review> reviews = reviewService.getReviewsForMovie(movieId);

        assertEquals(2, reviews.size());
        verify(reviewRepository, times(1)).findByMovieId(movieId);
    }

    @Test
    void getReviewsByUser_success() {
        Review review2 = new Review(testUser.getId(), "anotherMovieId", 2, "Not bad.");
        when(reviewRepository.findByUserId(testUser.getId())).thenReturn(Arrays.asList(review, review2));

        List<Review> reviews = reviewService.getReviewsByUser(testUser.getId());

        assertEquals(2, reviews.size());
        verify(reviewRepository, times(1)).findByUserId(testUser.getId());
    }

    @Test
    void deleteReview_userIsAuthor_success() {
        when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));
        // review.getUserId() is testUser.getId(), which matches the authenticated user

        boolean deleted = reviewService.deleteReview(review.getId());

        assertTrue(deleted);
        verify(reviewRepository, times(1)).findById(review.getId());
        verify(reviewRepository, times(1)).deleteById(review.getId());
    }

    @Test
    void deleteReview_userIsNotAuthor_returnsFalse() {
        User anotherUser = new User("another", "another@example.com", "pass", "pic.png");
        anotherUser.setId("anotherUserId");
        
        Review reviewByAnother = new Review(anotherUser.getId(), movieId, 3, "From another user");
        reviewByAnother.setId("reviewByAnotherId");
        
        when(reviewRepository.findById(reviewByAnother.getId())).thenReturn(Optional.of(reviewByAnother));
        // Authenticated user is testUser (ID: userId123), review author is anotherUser (ID: anotherUserId)

        boolean deleted = reviewService.deleteReview(reviewByAnother.getId());

        assertFalse(deleted, "Should not be able to delete another user's review");
        verify(reviewRepository, times(1)).findById(reviewByAnother.getId());
        verify(reviewRepository, never()).deleteById(anyString());
    }
    
    @Test
    void deleteReview_userNotAuthenticatedOrFound_throwsRuntimeException() {
        // Simulate user not being found in repository after SecurityContextHolder gives email
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.empty());
        
        when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review)); // Review itself exists

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reviewService.deleteReview(review.getId());
        });
        
        assertEquals("User not found, cannot verify review ownership.", exception.getMessage());
        verify(reviewRepository, times(1)).findById(review.getId()); // Still checks if review exists
        verify(reviewRepository, never()).deleteById(anyString());
    }


    @Test
    void deleteReview_reviewNotFound_returnsFalse() {
        when(reviewRepository.findById("nonExistentReviewId")).thenReturn(Optional.empty());

        boolean deleted = reviewService.deleteReview("nonExistentReviewId");

        assertFalse(deleted);
        verify(reviewRepository, times(1)).findById("nonExistentReviewId");
        verify(reviewRepository, never()).deleteById(anyString());
    }
}
