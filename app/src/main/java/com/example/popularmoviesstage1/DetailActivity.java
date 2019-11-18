package com.example.popularmoviesstage1;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.popularmoviesstage1.Data.FilmDbHelper;
import com.example.popularmoviesstage1.model.Film;
import com.example.popularmoviesstage1.utilities.NetworkUtils;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.squareup.picasso.Picasso;
import com.example.popularmoviesstage1.Data.FilmContract.FilmEntry;


import java.net.URL;
import java.util.ArrayList;

import com.example.popularmoviesstage1.ReviewAdapter.ReviewAdapterOnClickHandler;

import static com.example.popularmoviesstage1.Data.FilmContract.BASE_CONTENT_URI;
import static com.example.popularmoviesstage1.Data.FilmContract.FilmEntry.*;
import static com.example.popularmoviesstage1.Data.FilmContract.PATH_FILM;

public class DetailActivity extends YouTubeBaseActivity implements ReviewAdapterOnClickHandler {

    public static String YOUTUBE_API_KEY;

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
    private ImageView favoriteFilmButton;
    FilmDbHelper mDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        favoriteFilmButton = findViewById(R.id.iv_favButton);

        YOUTUBE_API_KEY = this.getString(R.string.youtube_api);
        mYoutubePlayerView =  findViewById(R.id.youtube_player_view);
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
        mDbHelper = new FilmDbHelper(this);

        String poster = film.getPoster();
        String filmUrl = NetworkUtils.buildPosterUrl(poster, NetworkUtils.ORIGINAL);
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
        if (hasObject(film.getId())) {
            favoriteFilmButton.setImageResource(R.drawable.ic_favorite_solid_24dp);
        }
        RecyclerView mRecyclerView =  findViewById(R.id.rv2);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new ReviewAdapter();
        new FetchTrailer().execute(film.getId(), "reviews");
        mRecyclerView.setAdapter(mAdapter);
        favoriteFilmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFavorite(film.getId());

            }
        });

    }

    public boolean hasObject(String id) {

        Log.v("yes ","I required the database");
        String[] projection ={FilmEntry._ID, COLUMN_FILM_TITLE,COLUMN_DATE,COLUMN_VOTE_AVERAGE,COLUMN_OVERVIEW,COLUMN_POSTER};
        Cursor cursor = getContentResolver().query(CONTENT_URI.buildUpon().appendPath(id).build(),projection,_ID + "=?",new String[]{id},null);
        boolean hasObject = false;
        if (cursor.moveToFirst()) {
            hasObject = true;
        }

        cursor.close();          // Don't forget to close your cursor
//        db.close();              //AND your Database!
        return hasObject;
    }

    @Override
    public void onClick(String reviewData) {

    }

    private void setFavorite(String filmId) {
        if (!hasObject(filmId)) {
            favoriteFilmButton.setImageResource(R.drawable.ic_favorite_solid_24dp);
            insertFilmInDatabase(film);

        } else {
            favoriteFilmButton.setImageResource(R.drawable.ic_favorite_border_black_24dp);
            deleteFilmFromDatabase(Integer.parseInt(film.getId()));
        }
    }

    private void deleteFilmFromDatabase(int filmId) {
        getContentResolver().delete(CONTENT_URI, _ID + "=?"  , new String[]{""+filmId});

    }

    private void insertFilmInDatabase(Film film) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(_ID, Integer.parseInt(film.getId()));
        contentValues.put(COLUMN_FILM_TITLE, film.getTitle());
        contentValues.put(COLUMN_DATE, film.getReleaseDate());
        contentValues.put(COLUMN_VOTE_AVERAGE, film.getVoteAverage());
        contentValues.put(COLUMN_OVERVIEW, film.getOverview());
        contentValues.put(COLUMN_POSTER, film.getPoster());
        Uri newUri = getContentResolver().insert(CONTENT_URI,  contentValues);
        if (newUri == null) {
            // If the new content URI is null, then there was an error with insertion.
            Toast.makeText(this, getString(R.string.editor_insert_film_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.editor_insert_film_successful),
                    Toast.LENGTH_SHORT).show();
        }

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
            String videosOrReviews = params[1];
            boolean isVideo = videosOrReviews.equals("videos");
            URL keyUrl = NetworkUtils.creatingKeyUrl(DetailActivity.this, id, videosOrReviews);

            try {
                String jsonKeysResponse = NetworkUtils
                        .getResponseFromHttpUrl(keyUrl);
                ArrayList<String> simpleJsonKeysData;
                simpleJsonKeysData = NetworkUtils
                        .extractKeysFromJson(DetailActivity.this, jsonKeysResponse, videosOrReviews);
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
                } else {
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