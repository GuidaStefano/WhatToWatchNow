package com.example.whattowatchnow.controller;

import com.example.whattowatchnow.domain.Review;
import com.example.whattowatchnow.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/movies/{movieId}/reviews")
    public ResponseEntity<Review> addReview(@PathVariable String movieId, @RequestBody Review review) {
        try {
            // The ReviewService.addReview method will extract the userId from the security context
            Review savedReview = reviewService.addReview(review, movieId);
            return new ResponseEntity<>(savedReview, HttpStatus.CREATED);
        } catch (RuntimeException e) { // Catch specific exceptions like UserNotFound
             return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED); // Or BAD_REQUEST if input is bad
        }
    }

    @GetMapping("/movies/{movieId}/reviews")
    public ResponseEntity<List<Review>> getReviewsForMovie(@PathVariable String movieId) {
        List<Review> reviews = reviewService.getReviewsForMovie(movieId);
        return new ResponseEntity<>(reviews, HttpStatus.OK);
    }

    @GetMapping("/users/{userId}/reviews")
    public ResponseEntity<List<Review>> getReviewsByUser(@PathVariable String userId) {
        List<Review> reviews = reviewService.getReviewsByUser(userId);
        return new ResponseEntity<>(reviews, HttpStatus.OK);
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable String reviewId) {
        // The ReviewService.deleteReview method will check if the current user is the author
        try {
            boolean deleted = reviewService.deleteReview(reviewId);
            if (deleted) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                // Could be because review not found or user not authorized
                return new ResponseEntity<>(HttpStatus.FORBIDDEN); // Or NOT_FOUND
            }
        } catch (RuntimeException e) { // Catch specific exceptions like UserNotFound
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }
}
