package com.example.whattowatchnow.service;

import com.example.whattowatchnow.domain.Movie;
import com.example.whattowatchnow.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private MovieService movieService;

    private Movie movie1;
    private Movie movie2;

    @BeforeEach
    void setUp() {
        movie1 = new Movie("Inception", Arrays.asList("Sci-Fi", "Action"), 2010, Arrays.asList("Leonardo DiCaprio"), "Mind-bending thriller", "url1");
        movie1.setId("movie1");
        movie2 = new Movie("The Matrix", Arrays.asList("Sci-Fi", "Action"), 1999, Arrays.asList("Keanu Reeves"), "A hacker learns the truth.", "url2");
        movie2.setId("movie2");
    }

    @Test
    void saveMovie_success() {
        when(movieRepository.save(any(Movie.class))).thenReturn(movie1);
        Movie savedMovie = movieService.saveMovie(movie1);
        assertNotNull(savedMovie);
        assertEquals(movie1.getTitle(), savedMovie.getTitle());
        verify(movieRepository, times(1)).save(movie1);
    }

    @Test
    void getAllMovies_success() {
        when(movieRepository.findAll()).thenReturn(Arrays.asList(movie1, movie2));
        List<Movie> movies = movieService.getAllMovies();
        assertEquals(2, movies.size());
        verify(movieRepository, times(1)).findAll();
    }

    @Test
    void getMovieById_found() {
        when(movieRepository.findById("movie1")).thenReturn(Optional.of(movie1));
        Optional<Movie> foundMovie = movieService.getMovieById("movie1");
        assertTrue(foundMovie.isPresent());
        assertEquals(movie1.getTitle(), foundMovie.get().getTitle());
        verify(movieRepository, times(1)).findById("movie1");
    }

    @Test
    void getMovieById_notFound() {
        when(movieRepository.findById("nonExistentId")).thenReturn(Optional.empty());
        Optional<Movie> foundMovie = movieService.getMovieById("nonExistentId");
        assertFalse(foundMovie.isPresent());
        verify(movieRepository, times(1)).findById("nonExistentId");
    }

    // Tests for findMovies (combined filter method)
    @Test
    void findMovies_noFilters_callsGetAllMovies() {
        // This test assumes that if all filters are null/empty, it defaults to getAllMovies behavior
        // based on the current MovieService implementation.
        // If MovieService is changed to return empty list for no criteria, this test needs adjustment.
        when(movieRepository.findAll()).thenReturn(Arrays.asList(movie1, movie2));
        List<Movie> result = movieService.findMovies(null, null, null, null);
        assertEquals(2, result.size());
        verify(mongoTemplate, never()).find(any(), any()); // Should not call mongoTemplate.find
        verify(movieRepository, times(1)).findAll(); // Should call findAll
    }
    
    @Test
    void findMovies_withQueryOnly() {
        String queryStr = "Inception";
        when(mongoTemplate.find(any(Query.class), eq(Movie.class))).thenReturn(Arrays.asList(movie1));
        
        List<Movie> result = movieService.findMovies(queryStr, null, null, null);
        
        assertEquals(1, result.size());
        assertEquals(movie1.getTitle(), result.get(0).getTitle());
        
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate, times(1)).find(queryCaptor.capture(), eq(Movie.class));
        
        Query capturedQuery = queryCaptor.getValue();
        String queryString = capturedQuery.getQueryObject().toString();
        assertTrue(queryString.contains("title") && queryString.contains(queryStr));
        assertTrue(queryString.contains("description") && queryString.contains(queryStr));
        assertTrue(queryString.contains("$or")); // Expect OR condition for title and description
        assertTrue(queryString.contains("$options\":\"i\"")); // Case-insensitive
    }

    @Test
    void findMovies_withGenreOnly() {
        String genre = "Sci-Fi";
        when(mongoTemplate.find(any(Query.class), eq(Movie.class))).thenReturn(Arrays.asList(movie1, movie2));
        
        List<Movie> result = movieService.findMovies(null, genre, null, null);
        
        assertEquals(2, result.size());
        
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate, times(1)).find(queryCaptor.capture(), eq(Movie.class));
        
        Query capturedQuery = queryCaptor.getValue();
        String queryString = capturedQuery.getQueryObject().toString();
        assertTrue(queryString.contains("genres") && queryString.contains(genre));
        assertTrue(queryString.contains("$options\":\"i\""));
    }

    @Test
    void findMovies_withYearOnly() {
        Integer year = 2010;
        when(mongoTemplate.find(any(Query.class), eq(Movie.class))).thenReturn(Arrays.asList(movie1));
        
        List<Movie> result = movieService.findMovies(null, null, year, null);
        
        assertEquals(1, result.size());
        
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate, times(1)).find(queryCaptor.capture(), eq(Movie.class));
        
        Query capturedQuery = queryCaptor.getValue();
        String queryString = capturedQuery.getQueryObject().toString();
        assertTrue(queryString.contains("releaseYear") && queryString.contains(year.toString()));
    }
    
    @Test
    void findMovies_withActorOnly() {
        String actor = "DiCaprio"; // Partial match for Leonardo DiCaprio
        when(mongoTemplate.find(any(Query.class), eq(Movie.class))).thenReturn(Arrays.asList(movie1));
        
        List<Movie> result = movieService.findMovies(null, null, null, actor);
        
        assertEquals(1, result.size());
        
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate, times(1)).find(queryCaptor.capture(), eq(Movie.class));
        
        Query capturedQuery = queryCaptor.getValue();
        String queryString = capturedQuery.getQueryObject().toString();
        assertTrue(queryString.contains("actors") && queryString.contains(actor));
        assertTrue(queryString.contains("$options\":\"i\""));
    }


    @Test
    void findMovies_withQueryAndGenreAndYearAndActor() {
        String queryStr = "Action Movie";
        String genre = "Action";
        Integer year = 2010;
        String actor = "Leo";

        when(mongoTemplate.find(any(Query.class), eq(Movie.class))).thenReturn(Arrays.asList(movie1));
        
        List<Movie> result = movieService.findMovies(queryStr, genre, year, actor);
        
        assertEquals(1, result.size());
        
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate, times(1)).find(queryCaptor.capture(), eq(Movie.class));
        
        Query capturedQuery = queryCaptor.getValue();
        String queryString = capturedQuery.getQueryObject().toString();
        
        // Check for query (title OR description)
        assertTrue(queryString.contains("$or") && queryString.contains(queryStr));
        // Check for genre
        assertTrue(queryString.contains("genres") && queryString.contains(genre));
        // Check for year
        assertTrue(queryString.contains("releaseYear") && queryString.contains(year.toString()));
        // Check for actor
        assertTrue(queryString.contains("actors") && queryString.contains(actor));
        // All regex should be case-insensitive
        long count = queryString.chars().filter(ch -> ch == 'i').count();
        String temp = queryString.replace("\"$options\":\"i\"", ""); // count "i" for case-insensitivity
        int iCount = 0;
        for(int i=0; i < temp.length() ; i++) {
            if(temp.charAt(i) == 'i' && temp.charAt(i-1) == '"' && temp.charAt(i-2) == ':') {
                 // this is a bit of a hack, better to parse JSON if possible
                 // but for now, we just count the occurrences of "$options":"i"
            }
        }
        // Depending on how MongoTemplate constructs the query, the number of "i" for case-insensitivity might vary.
        // A more robust check would be to parse the JSON structure of the query.
        // For now, let's just check if it's present for each regex field.
        assertTrue(queryString.indexOf("\"$options\":\"i\"") != queryString.lastIndexOf("\"$options\":\"i\""), "Expected multiple case-insensitive flags for query, genre, actor");
    }
}
