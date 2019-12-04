package com.example.popularmoviesstage1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.popularmoviesstage1.Data.FilmDbHelper;
import com.example.popularmoviesstage1.model.Film;
import com.example.popularmoviesstage1.utilities.NetworkUtils;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.google.android.youtube.player.YouTubePlayerView;
import com.squareup.picasso.Picasso;
import com.example.popularmoviesstage1.Data.FilmContract.FilmEntry;

import java.net.URL;
import java.util.ArrayList;

import com.example.popularmoviesstage1.ReviewAdapter.ReviewAdapterOnClickHandler;

import static com.example.popularmoviesstage1.Data.FilmContract.FilmEntry.*;

public class DetailActivity extends AppCompatActivity implements ReviewAdapterOnClickHandler, LoaderManager.LoaderCallbacks<DetailActivity.Passed> {

    private static final String TAG = DetailActivity.class.getSimpleName();

    ImageView imageView;
    TextView title;
    TextView date;
    TextView overview;
    TextView voteAverage;
    private ImageView favoriteFilmButton;

    Film film;
    private ReviewAdapter mAdapter;
    Context context;

    private RecyclerView recyclerView;
    //youtube player fragment
    private YouTubePlayerFragment youTubePlayerFragment;
    private ArrayList<String> youtubeVideoArrayList;

    //youtube player to play video when new video selected
    private YouTubePlayer youTubePlayer;
    boolean insideDB = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        context = getBaseContext();

        favoriteFilmButton = findViewById(R.id.iv_favButton);
        imageView = findViewById(R.id.film_image);

        film = (Film) getIntent().getSerializableExtra("FilmClass");
        Bundle filmBundle2 = new Bundle();
        filmBundle2.putString("film", film.getId());
        LoaderManager loaderManager = LoaderManager.getInstance(this);

        Loader<Passed> loader = loaderManager.getLoader(Integer.parseInt(film.getId()));
        if (loader == null) {
            loaderManager.initLoader(Integer.parseInt(film.getId()), filmBundle2, this);
        } else {
            loaderManager.restartLoader(Integer.parseInt(film.getId()), filmBundle2, this);
        }

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
        RecyclerView mRecyclerView = findViewById(R.id.rv2);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new ReviewAdapter();
        mRecyclerView.setAdapter(mAdapter);
        favoriteFilmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFavorite(film.getId());

            }
        });
        //TODO making videos running automatic by comparing it to the previous settings
        //TODO making list videos response to click after turning to landscape mode

    }

    /**
     * initialize youtube player via Fragment and get instance of YoutubePlayer
     */
    private void initializeYoutubePlayer() {
        youTubePlayerFragment = (YouTubePlayerFragment) getFragmentManager().findFragmentById(R.id.youtube_player_fragment);

        if (youTubePlayerFragment == null)
            return;

        youTubePlayerFragment.initialize(context.getString(R.string.youtube_api), new YouTubePlayer.OnInitializedListener() {


            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player,
                                                boolean wasRestored) {
                if (!wasRestored) {
                    youTubePlayer = player;

                    //set the player style default
                    youTubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.DEFAULT);

                    //youTubePlayer.loadVideos(youtubeVideoArrayList);
                    //cue the 1st video by default
                   youTubePlayer.loadVideos(youtubeVideoArrayList);
                   // youTubePlayer.loadPlaylist(youtubeVideoArrayList.get(0));


                }
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider arg0, YouTubeInitializationResult arg1) {

                //print or show error if initialization failed
                Log.e(TAG, "Youtube Player View initialization failed");
            }

        });

    }

    /**
     * setup the recycler view here
     */
    private void setUpRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);

        //Horizontal direction recycler view
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
    }

    /**
     * populate the recycler view and implement the click event here
     */
    private void populateRecyclerView() {
        final YoutubeVideoAdapter adapter = new YoutubeVideoAdapter(this, youtubeVideoArrayList);
        recyclerView.setAdapter(adapter);

        //set click event
        recyclerView.addOnItemTouchListener(new RecyclerViewOnClickListener(this, new RecyclerViewOnClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                if (youTubePlayerFragment != null && youTubePlayer != null) {
                    //update selected position
                    adapter.setSelectedPosition(position);

                    //youTubePlayer.release();
                    //ArrayList<String> youtubeVideoArrayList2= new ArrayList<>(youtubeVideoArrayList.subList(position,youtubeVideoArrayList.size()));

                    //load selected video
                    youTubePlayer.loadVideo(youtubeVideoArrayList.get(position));

                }

            }
        }));

    }


    @Override
    public void onClick(String reviewData) {

    }

    private void setFavorite(String filmId) {
        if (!insideDB) {
            favoriteFilmButton.setImageResource(R.drawable.ic_favorite_solid_24dp);
            insertFilmInDatabase(film);

        } else {
            favoriteFilmButton.setImageResource(R.drawable.ic_favorite_border_black_24dp);
            deleteFilmFromDatabase(Integer.parseInt(film.getId()));
        }
        insideDB = !insideDB;
    }

    private void deleteFilmFromDatabase(int filmId) {
        getContentResolver().delete(CONTENT_URI, _ID + "=?", new String[]{"" + filmId});

    }

    private void insertFilmInDatabase(Film film) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(_ID, Integer.parseInt(film.getId()));
        contentValues.put(COLUMN_FILM_TITLE, film.getTitle());
        contentValues.put(COLUMN_DATE, film.getReleaseDate());
        contentValues.put(COLUMN_VOTE_AVERAGE, film.getVoteAverage());
        contentValues.put(COLUMN_OVERVIEW, film.getOverview());
        contentValues.put(COLUMN_POSTER, film.getPoster());
        Uri newUri = getContentResolver().insert(CONTENT_URI, contentValues);
        if (newUri == null) {
            // If the new content URI is null, then there was an error with insertion.
            Toast.makeText(this, getString(R.string.editor_insert_film_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.editor_insert_film_successful),
                    Toast.LENGTH_SHORT).show();
        }

    }

    @NonNull
    @Override
    public Loader<Passed> onCreateLoader(int id, @Nullable final Bundle args) {
        return new AsyncTaskLoader<Passed>(this) {
            @Override
            protected void onStartLoading() {
                super.onStartLoading();
                forceLoad();
            }

            @Nullable
            @Override
            public Passed loadInBackground() {
                String filmId = args.getString("film");

                //getting the films of the new page
                String[] projection = {FilmEntry._ID};
                Cursor cursor = getContentResolver().query(CONTENT_URI.buildUpon().appendPath(filmId).build(), projection, _ID + "=?", new String[]{filmId}, null);
                boolean hasObject = false;
                if (cursor.moveToFirst()) {
                    hasObject = true;
                }

                cursor.close();

                ArrayList<String> simpleJsonKeysData = new ArrayList<>();
                ArrayList<String> simpleJsonKeysData2 = new ArrayList<>();


                URL keyUrl = NetworkUtils.creatingKeyUrl(DetailActivity.this, filmId, "videos");
                URL keyUrl2 = NetworkUtils.creatingKeyUrl(DetailActivity.this, filmId, "reviews");

                try {
                    String jsonKeysResponse = NetworkUtils
                            .getResponseFromHttpUrl(keyUrl);
                    simpleJsonKeysData = NetworkUtils
                            .extractKeysFromJson(DetailActivity.this, jsonKeysResponse, "videos");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    String jsonKeysResponse = NetworkUtils
                            .getResponseFromHttpUrl(keyUrl2);
                    simpleJsonKeysData2 = NetworkUtils
                            .extractKeysFromJson(DetailActivity.this, jsonKeysResponse, "reviews");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return new Passed(simpleJsonKeysData, simpleJsonKeysData2,hasObject);
            }

        };
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Passed> loader, Passed passed) {
        if (passed != null) {
            if (passed.JSONData != null) {
                youtubeVideoArrayList = passed.JSONData;
                initializeYoutubePlayer();
                setUpRecyclerView();
                populateRecyclerView();
            }
            if (passed.JSONData2 != null) {
                mAdapter.setReviewData(passed.JSONData2);
            }
        } else {
            //TODO show error message
        }
        if (passed.insideDB) {
            insideDB = passed.insideDB;
            favoriteFilmButton.setImageResource(R.drawable.ic_favorite_solid_24dp);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Passed> loader) {

    }

    class Passed {
        boolean insideDB;
        ArrayList<String> JSONData;
        ArrayList<String> JSONData2;

        Passed(ArrayList<String> JSONData, ArrayList<String> JSONData2,boolean insideDB) {
            this.insideDB= insideDB;
            this.JSONData2 = JSONData2;
            this.JSONData = JSONData;
        }
    }
}