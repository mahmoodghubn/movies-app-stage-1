package com.example.popularmoviesstage1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.popularmoviesstage1.model.Film;
import com.example.popularmoviesstage1.utilities.NetworkUtils;
import com.squareup.picasso.Picasso;

import java.net.URL;
import java.util.ArrayList;

public class DetailActivity extends AppCompatActivity {

    ImageView imageView;
    TextView title;
    TextView date;
    TextView overview;
    TextView voteAverage;
    TextView trailerUrlTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Intent intent = getIntent();
        imageView = findViewById(R.id.film_image);
        Film film = (Film) getIntent().getSerializableExtra("FilmClass");

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
        trailerUrlTextView = findViewById(R.id.film_url);
        new FetchTrailer().execute(film.getId());

    }

    private class FetchTrailer extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            /* If there's no zip code, there's nothing to look up. */
            if (params.length == 0) {
                return null;
            }

            String s;
            String id = params[0];
            URL keyUrl = NetworkUtils.creatingKeyUrl(id);

            try {
                String jsonKeysResponse = NetworkUtils
                        .getResponseFromHttpUrl(keyUrl);

                ArrayList<String> simpleJsonKeysData = NetworkUtils
                        .extractKeysFromJson(DetailActivity.this, jsonKeysResponse);
                URL trailerUrl = NetworkUtils.creatingTrailerUrl(simpleJsonKeysData.get(0));
                s = trailerUrl.toString();

                return s;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String trailerUrl){
            if (trailerUrl != null) {
                TextView trailerUrlTextView = (TextView)findViewById(R.id.film_url);
                trailerUrlTextView.setText(trailerUrl);
            } else {
                //TODO show error message
            }
        }
    }
}