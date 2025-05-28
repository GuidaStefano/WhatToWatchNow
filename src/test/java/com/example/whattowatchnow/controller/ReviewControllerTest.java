package com.example.whattowatchnow.controller;

import com.example.whattowatchnow.domain.Review;
import com.example.whattowatchnow.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
public class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @Autowired
    private ObjectMapper objectMapper;

    private Review review1;
    private Review review2;
    private String movieId = "movie123";
    private String userId = "user123";

    @BeforeEach
    void setUp() {
        review1 = new Review(userId, movieId, 5, "Excellent!");
        review1.setId("reviewId1");
        review1.setReviewDate(LocalDateTime.now());

        review2 = new Review(userId, movieId, 4, "Very good.");
        review2.setId("reviewId2");
        review2.setReviewDate(LocalDateTime.now().minusDays(1));
    }

    @Test
    @WithMockUser // Required for adding a review
    void addReview_success_returnsCreatedReview() throws Exception {
        when(reviewService.addReview(any(Review.class), eq(movieId))).thenReturn(review1);

        Review reviewPayload = new Review(null, null, 5, "Excellent!"); // userId and movieId are set by service/path

        mockMvc.perform(post("/api/movies/{movieId}/reviews", movieId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(review1.getId())))
                .andExpect(jsonPath("$.comment", is(review1.getComment())));
    }
    
    @Test
    @WithMockUser
    void addReview_serviceThrowsRuntimeException_returnsUnauthorizedOrBadRequest() throws Exception {
        // Assuming the service might throw RuntimeException for various reasons (e.g., user not found from context)
        when(reviewService.addReview(any(Review.class), eq(movieId))).thenThrow(new RuntimeException("User context error"));

        Review reviewPayload = new Review(null, null, 5, "This will fail");

        mockMvc.perform(post("/api/movies/{movieId}/reviews", movieId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewPayload)))
                .andExpect(status().isUnauthorized()); // Controller maps RuntimeException to UNAUTHORIZED
    }


    @Test
    void getReviewsForMovie_success_returnsListOfReviews() throws Exception {
        List<Review> reviews = Arrays.asList(review1, review2);
        when(reviewService.getReviewsForMovie(movieId)).thenReturn(reviews);

        mockMvc.perform(get("/api/movies/{movieId}/reviews", movieId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(review1.getId())))
                .andExpect(jsonPath("$[1].id", is(review2.getId())));
    }
    
    @Test
    void getReviewsForMovie_noReviews_returnsEmptyList() throws Exception {
        when(reviewService.getReviewsForMovie(movieId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/movies/{movieId}/reviews", movieId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }


    @Test
    @WithMockUser // Required for getting reviews by user ID
    void getReviewsByUser_success_returnsListOfReviews() throws Exception {
        List<Review> reviews = Arrays.asList(review1, review2);
        when(reviewService.getReviewsByUser(userId)).thenReturn(reviews);

        mockMvc.perform(get("/api/users/{userId}/reviews", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userId", is(userId)))
                .andExpect(jsonPath("$[1].userId", is(userId)));
    }

    @Test
    @WithMockUser
    void deleteReview_success_returnsNoContent() throws Exception {
        String reviewIdToDelete = "reviewId1";
        when(reviewService.deleteReview(reviewIdToDelete)).thenReturn(true);

        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewIdToDelete)
                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void deleteReview_notAuthorizedOrNotFound_returnsForbidden() throws Exception {
        String reviewIdToDelete = "reviewId1";
        when(reviewService.deleteReview(reviewIdToDelete)).thenReturn(false); // Service indicates deletion failed (e.g. not owner, or not found)

        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewIdToDelete)
                .with(csrf()))
                .andExpect(status().isForbidden());
    }
    
    @Test
    @WithMockUser
    void deleteReview_serviceThrowsRuntimeException_returnsUnauthorized() throws Exception {
        String reviewIdToDelete = "reviewId1";
        when(reviewService.deleteReview(reviewIdToDelete)).thenThrow(new RuntimeException("User context error"));

        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewIdToDelete)
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void addReview_unauthenticated_returnsUnauthorized() throws Exception {
        Review reviewPayload = new Review(null, null, 5, "Excellent!");
        mockMvc.perform(post("/api/movies/{movieId}/reviews", movieId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewPayload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getReviewsByUser_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/reviews", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void deleteReview_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/reviews/{reviewId}", "reviewId1")
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
