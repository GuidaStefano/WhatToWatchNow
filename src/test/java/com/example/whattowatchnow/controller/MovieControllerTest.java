package com.example.whattowatchnow.controller;

import com.example.whattowatchnow.domain.Movie;
import com.example.whattowatchnow.service.MovieService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser; // For POST endpoint
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MovieController.class)
public class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovieService movieService;
    
    // As MovieController has GET /api/movies/** as permitAll,
    // we don't always need @WithMockUser for GETs unless a specific user role is tested.
    // For POST /api/movies, it's authenticated, so @WithMockUser is needed.

    @Autowired
    private ObjectMapper objectMapper;

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
    void getAllMovies_noParams_returnsListOfMovies() throws Exception {
        List<Movie> allMovies = Arrays.asList(movie1, movie2);
        when(movieService.findMovies(isNull(), isNull(), isNull(), isNull())).thenReturn(allMovies);

        mockMvc.perform(get("/api/movies")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is(movie1.getTitle())))
                .andExpect(jsonPath("$[1].title", is(movie2.getTitle())));
    }

    @Test
    void getAllMovies_withParams_returnsFilteredMovies() throws Exception {
        String query = "Inception";
        String genre = "Sci-Fi";
        Integer year = 2010;
        
        when(movieService.findMovies(eq(query), eq(genre), eq(year), isNull())).thenReturn(Collections.singletonList(movie1));

        mockMvc.perform(get("/api/movies")
                .param("query", query)
                .param("genre", genre)
                .param("year", year.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is(movie1.getTitle())));
    }

    @Test
    void getMovieById_found_returnsMovie() throws Exception {
        when(movieService.getMovieById("movie1")).thenReturn(Optional.of(movie1));

        mockMvc.perform(get("/api/movies/movie1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(movie1.getId())))
                .andExpect(jsonPath("$.title", is(movie1.getTitle())));
    }

    @Test
    void getMovieById_notFound_returnsNotFound() throws Exception {
        when(movieService.getMovieById("nonExistentId")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/movies/nonExistentId")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser // POST /api/movies requires authentication
    void addMovie_success_returnsCreatedMovie() throws Exception {
        when(movieService.saveMovie(any(Movie.class))).thenReturn(movie1);

        mockMvc.perform(post("/api/movies")
                .with(csrf()) // Add CSRF token
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(movie1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(movie1.getId())))
                .andExpect(jsonPath("$.title", is(movie1.getTitle())));
    }
    
    @Test
    @WithMockUser 
    void addMovie_serviceThrowsException_returnsInternalServerError() throws Exception {
        when(movieService.saveMovie(any(Movie.class))).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/api/movies")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(movie1)))
                .andExpect(status().isInternalServerError());
    }
    
    @Test
    void addMovie_unauthenticated_returnsUnauthorized() throws Exception {
        // No @WithMockUser, so request is anonymous
        mockMvc.perform(post("/api/movies")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(movie1)))
                .andExpect(status().isUnauthorized()); // Or 403 Forbidden if CSRF is main issue without auth
                // Spring Security default is 401 if not authenticated for a secured endpoint.
    }
}
