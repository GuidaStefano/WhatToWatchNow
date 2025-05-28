package com.example.whattowatchnow.repository;

import com.example.whattowatchnow.domain.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByMovieId(String movieId);
    List<Review> findByUserId(String userId);
}
