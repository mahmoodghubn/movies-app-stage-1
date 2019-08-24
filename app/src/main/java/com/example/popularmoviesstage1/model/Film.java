package com.example.popularmoviesstage1.model;


import java.io.Serializable;

public class Film implements Serializable {
    private String poster;
    private String title;
    private String releaseDate;
    private String voteAverage;
    private String overview;

    public Film(String poster,String title, String overview, String releaseDate,String voteAverage ){
        setPoster(poster);
        setOverview(overview);
        setReleaseDate(releaseDate);
        setTitle(title);
        setVoteAverage(voteAverage);

    }
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getVoteAverage() {
        return voteAverage;
    }

    public void setVoteAverage(String voteAverage) {
        this.voteAverage = voteAverage;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }



    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }
}
