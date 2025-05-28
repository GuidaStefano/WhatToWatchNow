package com.example.whattowatchnow.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "movies")
public class Movie {

    @Id
    private String id;

    private String title;
    private List<String> genres;
    private Integer releaseYear;
    private List<String> actors;
    private String description;
    private String posterUrl; // Optional

    // Constructors
    public Movie() {
    }

    public Movie(String title, List<String> genres, Integer releaseYear, List<String> actors, String description, String posterUrl) {
        this.title = title;
        this.genres = genres;
        this.releaseYear = releaseYear;
        this.actors = actors;
        this.description = description;
        this.posterUrl = posterUrl;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public List<String> getActors() {
        return actors;
    }

    public void setActors(List<String> actors) {
        this.actors = actors;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    // toString (optional but good practice)
    @Override
    public String toString() {
        return "Movie{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", genres=" + genres +
                ", releaseYear=" + releaseYear +
                ", actors=" + actors +
                ", description='" + description + '\'' +
                ", posterUrl='" + posterUrl + '\'' +
                '}';
    }
}
