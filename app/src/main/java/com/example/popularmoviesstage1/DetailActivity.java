
package com.example.popularmoviesstage1;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.popularmoviesstage1.model.Film;
import com.example.popularmoviesstage1.utilities.NetworkUtils;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.squareup.picasso.Picasso;

import java.net.URL;
import java.util.ArrayList;

public class DetailActivity extends YouTubeBaseActivity  {

    public static final String YOUTUBE_API_KEY = "AIzaSyD51qd_0eGvR-YJpM9hwDnd5U9wHiH-ZTM";

    ImageView imageView;
    TextView title;
    TextView date;
    TextView overview;
    TextView voteAverage;
    YouTubePlayerView mYoutubePlayerView;

    YouTubePlayer.OnInitializedListener mOnInitializedListener;
    String loadVideo;
    Film film;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        mYoutubePlayerView = (YouTubePlayerView)findViewById(R.id.youtube_player_view);
        imageView = findViewById(R.id.film_image);
        mOnInitializedListener = new YouTubePlayer.OnInitializedListener(){


            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
                youTubePlayer.loadVideo(loadVideo);

            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {

            }
        };

        film = (Film) getIntent().getSerializableExtra("FilmClass");
        new FetchTrailer().execute(film.getId());

        String filmUrl = film.getPoster();
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
    }

    private class FetchTrailer extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            /* If there's no zip code, there's nothing to look up. */
            if (params.length == 0) {
                return null;
            }

            String youtubeUrl;
            String id = params[0];
            URL keyUrl = NetworkUtils.creatingKeyUrl(id);

            try {
                String jsonKeysResponse = NetworkUtils
                        .getResponseFromHttpUrl(keyUrl);

                ArrayList<String> simpleJsonKeysData = NetworkUtils
                        .extractKeysFromJson(DetailActivity.this, jsonKeysResponse);

                youtubeUrl = simpleJsonKeysData.get(0);
                return youtubeUrl;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String trailerUrl){
            if (trailerUrl != null) {
                loadVideo = trailerUrl;
                mYoutubePlayerView.initialize(YOUTUBE_API_KEY,mOnInitializedListener);

            } else {
                //TODO show error message
            }
        }
    }
}