package com.example.whattowatchnow.service;

import com.example.whattowatchnow.domain.Review;
import com.example.whattowatchnow.domain.User;
import com.example.whattowatchnow.repository.ReviewRepository;
import com.example.whattowatchnow.repository.UserRepository; // For fetching user details
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository; // To fetch User object if needed

    @Autowired
    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    public Review addReview(Review review, String movieId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName(); // email is used as username

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found, cannot add review."));

        review.setUserId(currentUser.getId());
        review.setMovieId(movieId);
        review.setReviewDate(LocalDateTime.now());
        return reviewRepository.save(review);
    }

    public List<Review> getReviewsForMovie(String movieId) {
        return reviewRepository.findByMovieId(movieId);
    }

    public List<Review> getReviewsByUser(String userId) {
        return reviewRepository.findByUserId(userId);
    }

    public boolean deleteReview(String reviewId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found, cannot verify review ownership."));

        Optional<Review> reviewOptional = reviewRepository.findById(reviewId);
        if (reviewOptional.isPresent()) {
            Review review = reviewOptional.get();
            // Check if the current user is the author of the review
            // (Later, admin role could also be allowed to delete)
            if (review.getUserId().equals(currentUser.getId())) {
                reviewRepository.deleteById(reviewId);
                return true;
            } else {
                // User is not the author, cannot delete
                // Consider throwing a specific security exception
                return false; 
            }
        }
        return false; // Review not found
    }
}
