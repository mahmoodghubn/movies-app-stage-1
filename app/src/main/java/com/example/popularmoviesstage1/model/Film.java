package com.example.popularmoviesstage1.model;

import java.io.Serializable;

public class Film implements Serializable {
    private String poster;
    private String title;
    private String releaseDate;
    private String voteAverage;
    private String overview;
    private String id;
    private String backdrop_path;

    public Film(String poster, String title, String overview, String releaseDate, String voteAverage,String id ,String backdrop_path){
        setPoster(poster);
        setOverview(overview);
        setReleaseDate(releaseDate);
        setTitle(title);
        setVoteAverage(voteAverage);
        setId(id);
        setBackdrop_path(backdrop_path);

    }
    public String getBackdrop_path() {
        return backdrop_path;
    }

    public void setBackdrop_path(String backdrop_path) {
        this.backdrop_path = backdrop_path;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    private void setTitle(String title) {
        this.title = title;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    private void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getVoteAverage() {
        return voteAverage;
    }

    private void setVoteAverage(String voteAverage) {
        this.voteAverage = voteAverage;
    }

    public String getOverview() {
        return overview;
    }

    private void setOverview(String overview) {
        this.overview = overview;
    }

    public String getPoster() {
        return poster;
    }

    private void setPoster(String poster) {
        this.poster = poster;
    }
}
