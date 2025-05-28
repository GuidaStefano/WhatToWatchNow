package com.example.whattowatchnow.repository;

import com.example.whattowatchnow.domain.Movie;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MovieRepository extends MongoRepository<Movie, String> {

    List<Movie> findByTitleContainingIgnoreCase(String title);

    List<Movie> findByGenresContainingIgnoreCase(String genre);

    List<Movie> findByReleaseYear(Integer year);

    List<Movie> findByActorsContainingIgnoreCase(String actor);

    // For combined filtering, we might need to use MongoTemplate or Querydsl for dynamic queries,
    // but for now, these individual methods can be used by the service layer.
    // A more advanced approach for findMovies in MovieService could involve @Query annotation with SpEL
    // or a custom repository implementation if the parameter combinations become too complex.
}
