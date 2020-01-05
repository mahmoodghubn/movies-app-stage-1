package com.example.popularmoviesstage1;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
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

import com.example.popularmoviesstage1.model.Film;
import com.example.popularmoviesstage1.utilities.NetworkUtils;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.squareup.picasso.Picasso;
import com.example.popularmoviesstage1.Data.FilmContract.FilmEntry;

import java.net.URL;
import java.util.ArrayList;

import com.example.popularmoviesstage1.ReviewAdapter.ReviewAdapterOnClickHandler;

import static android.os.SystemClock.sleep;
import static com.example.popularmoviesstage1.Data.FilmContract.FilmEntry.*;
import static com.example.popularmoviesstage1.MainActivity.isBrightMood;

public class DetailActivity extends AppCompatActivity implements ReviewAdapterOnClickHandler, LoaderManager.LoaderCallbacks<DetailActivity.Passed> {

    private static final String TAG = DetailActivity.class.getSimpleName();

    ImageView filmImageView;
    ImageView backdropImageView;
    TextView title;
    TextView date;
    TextView overview;
    RatingBar voteAverage;
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
    IntentFilter internetConnectionIntentFilter;

    InternetBroadCastReceiver internetBroadCastReceiver;
    boolean isDataLoaded;
    boolean isImageLoaded = false;
    private TextView emptyView;
    RecyclerView mRecyclerView;
    View loadingIndicator;
    //this boolean is to forbid the back online statement from appearing when app first launch and when resuming
    boolean whenAppLaunchFirstTime = true;
    Bundle filmBundle;
    String filmUrl;
    String backdropUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isBrightMood){
            setTheme(R.style.DetailActivity2);
        }else {
            setTheme(R.style.DetailActivity);
        }
        setContentView(R.layout.activity_detail);
        favoriteFilmButton = findViewById(R.id.iv_favButton);
        if (isBrightMood){
            favoriteFilmButton.setImageResource(R.drawable.ic_favorite_border_black_24dp);
        }else {
            favoriteFilmButton.setImageResource(R.drawable.ic_favorite_border_white_24dp);
        }

        emptyView =  findViewById(R.id.empty_view);
        loadingIndicator = findViewById(R.id.loading_indicator);
        context = getBaseContext();


        filmImageView = findViewById(R.id.film_image);
        backdropImageView = findViewById(R.id.film_backdrop);

        film = (Film) getIntent().getSerializableExtra("FilmClass");
        filmBundle = new Bundle();
        filmBundle.putString("film", film.getId());
        LoaderManager loaderManager = LoaderManager.getInstance(this);

        Loader<Passed> loader = loaderManager.getLoader(Integer.parseInt(film.getId()));
        if (loader == null) {
            loaderManager.initLoader(Integer.parseInt(film.getId()), filmBundle, this);
        } else {
            loaderManager.restartLoader(Integer.parseInt(film.getId()), filmBundle, this);
        }

        bindingData();

        mRecyclerView = findViewById(R.id.rv2);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new ReviewAdapter();
        mRecyclerView.setAdapter(mAdapter);
        favoriteFilmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFavorite();
            }
        });

        internetConnectionIntentFilter = new IntentFilter();
        internetConnectionIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        internetBroadCastReceiver = new InternetBroadCastReceiver();
    }

    private void bindingData() {
        String poster = film.getPoster();
        String backdrop = film.getBackdrop_path();
        filmUrl = NetworkUtils.buildPosterUrl(poster, NetworkUtils.w185);
        backdropUrl = NetworkUtils.buildPosterUrl(backdrop, NetworkUtils.ORIGINAL);

        // Get a reference to the ConnectivityManager to check state of network connectivity
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        // Get details on the currently active default data network
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        // If there is a network connection, fetch data
        if (networkInfo != null && networkInfo.isConnected()) {
            Picasso.with(this)
                    .load(filmUrl)
                    .into(filmImageView);
            Picasso.with(this)
                    .load(backdropUrl)
                    .into(backdropImageView);
            isImageLoaded = true;
        }

        title = findViewById(R.id.film_title);
        title.setText(film.getTitle());
        voteAverage = findViewById(R.id.vote_average);
        voteAverage.setRating(Float.valueOf(film.getVoteAverage()));

        date = findViewById(R.id.date);
        date.setText(film.getReleaseDate());
        overview = findViewById(R.id.overview);
        overview.setText(film.getOverview());
        setUpRecyclerView();
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

                    //load selected video
                    youTubePlayer.loadVideo(youtubeVideoArrayList.get(position));
                }
            }
        }));
    }

    private void setFavorite() {
        if (!insideDB) {
            favoriteFilmButton.setImageResource(R.drawable.ic_favorite_solid_24dp);
            insertFilmInDatabase(film,context);
        } else {
            if (isBrightMood){
                favoriteFilmButton.setImageResource(R.drawable.ic_favorite_border_black_24dp);
            }else {
                favoriteFilmButton.setImageResource(R.drawable.ic_favorite_border_white_24dp);
            }
            deleteFilmFromDatabase(film.getId(),context);
        }
        insideDB = !insideDB;
    }

    public boolean queryFilmFromDatabase(String filmId,Context context){
        String[] projection = {FilmEntry._ID};

        Cursor cursor = context.getContentResolver().query(CONTENT_URI.buildUpon().appendPath(filmId).build(), projection, _ID + "=?", new String[]{filmId}, null);
        assert cursor != null;
        boolean isInside = cursor.moveToFirst();
        cursor.close();
        return isInside;
    }
    public void deleteFilmFromDatabase(String filmId,Context context) {
        int deletedRows = context.getContentResolver().delete(CONTENT_URI, _ID + "=?", new String[]{ filmId});
        if (deletedRows == 0) {
            // If the new content URI is null, then there was an error with insertion.
            Toast.makeText(context, context.getString(R.string.editor_delete_film_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, context.getString(R.string.editor_delete_film_successful),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void insertFilmInDatabase(Film film,Context context) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(_ID, Integer.parseInt(film.getId()));
        contentValues.put(COLUMN_FILM_TITLE, film.getTitle());
        contentValues.put(COLUMN_DATE, film.getReleaseDate());
        contentValues.put(COLUMN_VOTE_AVERAGE, film.getVoteAverage());
        contentValues.put(COLUMN_OVERVIEW, film.getOverview());
        contentValues.put(COLUMN_BACKDROP_PATH, film.getBackdrop_path());
        contentValues.put(COLUMN_POSTER, film.getPoster());
        Uri newUri = context.getContentResolver().insert(CONTENT_URI, contentValues);
        if (newUri == null) {
            // If the new content URI is null, then there was an error with insertion.
            Toast.makeText(context, context.getString(R.string.editor_insert_film_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, context.getString(R.string.editor_insert_film_successful),
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

            @NonNull
            @Override
            public Passed loadInBackground() {
                assert args != null;
                String filmId = args.getString("film");
                boolean hasObject = queryFilmFromDatabase(filmId,context);
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
                return new Passed(simpleJsonKeysData, simpleJsonKeysData2, hasObject);
            }
        };
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Passed> loader, Passed passed) {
        loadingIndicator.setVisibility(View.GONE);
        if (passed != null) {
            isDataLoaded = true;
            if (passed.JSONData != null && passed.JSONData.size() != 0) {
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                youtubeVideoArrayList = passed.JSONData;
                initializeYoutubePlayer();
                populateRecyclerView();
            } else {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);

            }
            if (passed.JSONData2 != null && passed.JSONData2.size() != 0) {
                mAdapter.setReviewData(passed.JSONData2);
            }
            if (passed.JSONData.size() == 0 || passed.JSONData2.size() == 0) {
                isDataLoaded = false;
            }
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
        Passed(ArrayList<String> JSONData, ArrayList<String> JSONData2, boolean insideDB) {
            this.insideDB = insideDB;
            this.JSONData2 = JSONData2;
            this.JSONData = JSONData;
        }
    }

    public void showInternetConnection(boolean isConnected) {
        final TextView online_situation = findViewById(R.id.internet_situation);

        if (isConnected && !whenAppLaunchFirstTime) {
            online_situation.setVisibility(View.VISIBLE);
            online_situation.setText(R.string.back_online);
            online_situation.setBackgroundColor(getResources().getColor(R.color.online));
            CountDownTimer timer = new CountDownTimer(5000, 5000) {
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    online_situation.setVisibility(View.GONE);
                }
            };
            timer.start();

            if (!isDataLoaded) {
                LoaderManager loaderManager = LoaderManager.getInstance(this);
                loadingIndicator.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                Loader<Passed> loader = loaderManager.getLoader(Integer.parseInt(film.getId()));
                if (loader == null) {
                    loaderManager.initLoader(Integer.parseInt(film.getId()), filmBundle, this);
                } else {
                    loaderManager.restartLoader(Integer.parseInt(film.getId()), filmBundle, this);
                }
            }
            if (!isImageLoaded) {
                Picasso.with(this)
                        .load(filmUrl)
                        .into(filmImageView);
                Picasso.with(this)
                        .load(backdropUrl)
                        .into(backdropImageView);
                isImageLoaded = true;
            }
        } else if (!isConnected) {
            online_situation.setVisibility(View.VISIBLE);
            online_situation.setText(R.string.offline_message);
            online_situation.setBackgroundColor(getResources().getColor(R.color.cardview_dark_background));
        }
        whenAppLaunchFirstTime = false;
    }

    private class InternetBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                boolean isConnected = info.isConnected();
                sleep(1000);
                showInternetConnection(isConnected);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(internetBroadCastReceiver, internetConnectionIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(internetBroadCastReceiver);
        whenAppLaunchFirstTime = true;
    }
}