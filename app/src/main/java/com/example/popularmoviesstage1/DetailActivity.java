package com.example.popularmoviesstage1;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.popularmoviesstage1.model.Film;
import com.example.popularmoviesstage1.utilities.NetworkUtils;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.squareup.picasso.Picasso;

import java.net.URL;
import java.util.ArrayList;
import com.example.popularmoviesstage1.ReviewAdapter.ReviewAdapterOnClickHandler;

public class DetailActivity extends YouTubeBaseActivity implements ReviewAdapterOnClickHandler {

    public static final String YOUTUBE_API_KEY = "AIzaSyD51qd_0eGvR-YJpM9hwDnd5U9wHiH-ZTM";

    ImageView imageView;
    TextView title;
    TextView date;
    TextView overview;
    TextView voteAverage;
    YouTubePlayerView mYoutubePlayerView;

    YouTubePlayer.OnInitializedListener mOnInitializedListener;
    ArrayList<String> loadVideos;
    Film film;
    private ReviewAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        mYoutubePlayerView = (YouTubePlayerView) findViewById(R.id.youtube_player_view);
        imageView = findViewById(R.id.film_image);
        mOnInitializedListener = new YouTubePlayer.OnInitializedListener() {


            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
                youTubePlayer.loadVideos(loadVideos);

            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {

            }
        };

        film = (Film) getIntent().getSerializableExtra("FilmClass");
        new FetchTrailer().execute(film.getId(), "videos");

        String poster = film.getPoster();
        String filmUrl = NetworkUtils.buildPosterUrl(poster,NetworkUtils.ORIGINAL);
        Picasso.with(this)
                .load(filmUrl)
                .into(imageView);
        title = findViewById(R.id.film_title);
        title.setText(film.getTitle());
        voteAverage = findViewById(R.id.vote_average);
        voteAverage.setText(film.getVoteAverage());
        date = findViewById(R.id.date);
        date.setText(film.getReleaseDate());
        overview = findViewById(R.id.overview);
        overview.setText(film.getOverview());
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.rv2);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new ReviewAdapter();
        new FetchTrailer().execute(film.getId(),"reviews");
        mRecyclerView.setAdapter(mAdapter);

    }

    @Override
    public void onClick(String reviewData) {

    }

    @SuppressLint("StaticFieldLeak")
    private class FetchTrailer extends AsyncTask<String, Void, Passed> {

        @Override
        protected Passed doInBackground(String... params) {
            /* If there's no zip code, there's nothing to look up. */
            if (params.length == 0) {
                return null;
            }

            String id = params[0];
            String query = params[1];
            boolean isVideo = query.equals("videos");
            URL keyUrl = NetworkUtils.creatingKeyUrl(id, query);


            try {
                String jsonKeysResponse = NetworkUtils
                        .getResponseFromHttpUrl(keyUrl);
                ArrayList<String> simpleJsonKeysData;
                if (isVideo) {
                    simpleJsonKeysData = NetworkUtils
                            .extractKeysFromJson(DetailActivity.this, jsonKeysResponse, query);
                } else {
                    simpleJsonKeysData = NetworkUtils
                            .extractKeysFromJson(DetailActivity.this, jsonKeysResponse, query);

                }


                return new Passed(isVideo, simpleJsonKeysData);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Passed passed) {
            if (passed.JSONData != null) {
                if (passed.isVideos) {
                    loadVideos = passed.JSONData;
                    mYoutubePlayerView.initialize(YOUTUBE_API_KEY, mOnInitializedListener);
                }else {
                    mAdapter.setReviewData(passed.JSONData);

                }
            } else {
                //TODO show error message
            }
        }
    }

    private class Passed {
        boolean isVideos;
        ArrayList<String> JSONData;

        Passed(boolean isVideos, ArrayList<String> JSONData) {
            this.isVideos = isVideos;
            this.JSONData = JSONData;
        }
    }
}