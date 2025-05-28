package com.example.whattowatchnow.service;

import com.example.whattowatchnow.domain.Movie;
import com.example.whattowatchnow.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final MongoTemplate mongoTemplate; // For complex queries

    @Autowired
    public MovieService(MovieRepository movieRepository, MongoTemplate mongoTemplate) {
        this.movieRepository = movieRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public Movie saveMovie(Movie movie) {
        // Additional validation or business logic can go here
        return movieRepository.save(movie);
    }

    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    public Optional<Movie> getMovieById(String id) {
        return movieRepository.findById(id);
    }

    public List<Movie> searchMoviesByTitle(String title) {
        return movieRepository.findByTitleContainingIgnoreCase(title);
    }

    public List<Movie> filterMoviesByGenre(String genre) {
        return movieRepository.findByGenresContainingIgnoreCase(genre);
    }

    public List<Movie> filterMoviesByYear(Integer year) {
        return movieRepository.findByReleaseYear(year);
    }

    public List<Movie> filterMoviesByActor(String actor) {
        return movieRepository.findByActorsContainingIgnoreCase(actor);
    }

    /**
     * Finds movies based on a combination of query (title), genre, year, and actor.
     * Parameters that are null or empty are ignored.
     */
    public List<Movie> findMovies(String query, String genre, Integer year, String actor) {
        Query mongoQuery = new Query();
        Criteria criteria = new Criteria();
        boolean criteriaAdded = false;

        if (StringUtils.hasText(query)) {
            criteria.orOperator(
                Criteria.where("title").regex(query, "i"), // "i" for case-insensitive
                Criteria.where("description").regex(query, "i") 
            );
            criteriaAdded = true;
        }

        if (StringUtils.hasText(genre)) {
            criteria.and("genres").regex(genre, "i");
            criteriaAdded = true;
        }

        if (year != null) {
            criteria.and("releaseYear").is(year);
            criteriaAdded = true;
        }

        if (StringUtils.hasText(actor)) {
            criteria.and("actors").regex(actor, "i");
            criteriaAdded = true;
        }
        
        if (!criteriaAdded) {
            return getAllMovies(); // Or return empty list if no criteria means no search
        }

        mongoQuery.addCriteria(criteria);
        return mongoTemplate.find(mongoQuery, Movie.class);
    }
}
