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
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.databinding.DataBindingUtil;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.popularmoviesstage1.databinding.ActivityDetailBinding;
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
import static com.example.popularmoviesstage1.MainActivity.SHARED_ELEMENT_TRANSITION_EXTRA;
import static com.example.popularmoviesstage1.MainActivity.isBrightMood;

public class DetailActivity extends AppCompatActivity implements ReviewAdapterOnClickHandler, LoaderManager.LoaderCallbacks<DetailActivity.Passed> {

    private static final String TAG = DetailActivity.class.getSimpleName();
    ActivityDetailBinding mBinding;
    Film film;
    private ReviewAdapter mAdapter;
    Context context;
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
        mBinding = DataBindingUtil.setContentView(this,R.layout.activity_detail);
        //ViewCompat.setTransitionName(mBinding.filmPoster, SHARED_ELEMENT_TRANSITION_EXTRA);

        if (isBrightMood){
            mBinding.ivFavButton.setImageResource(R.drawable.ic_favorite_border_black_24dp);
        }else {
            mBinding.ivFavButton.setImageResource(R.drawable.ic_favorite_border_white_24dp);
        }

        context = getBaseContext();

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


        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        mBinding.recyclerView2.setLayoutManager(linearLayoutManager);
        mBinding.recyclerView2.setHasFixedSize(true);
        mAdapter = new ReviewAdapter();
        mBinding.recyclerView2.setAdapter(mAdapter);
        mBinding.ivFavButton.setOnClickListener(new View.OnClickListener() {
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
                    .into(mBinding.filmPoster);
            Picasso.with(this)
                    .load(backdropUrl)
                    .into(mBinding.filmBackdrop);
            isImageLoaded = true;
        }


        mBinding.filmTitle.setText(film.getTitle());
        mBinding.voteAverage.setRating(Float.valueOf(film.getVoteAverage()));
        mBinding.date.setText(film.getReleaseDate());
        mBinding.overview.setText(film.getOverview());
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

        mBinding.recyclerView.setHasFixedSize(true);

        //Horizontal direction recycler view
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mBinding.recyclerView.setLayoutManager(linearLayoutManager);
    }

    /**
     * populate the recycler view and implement the click event here
     */
    private void populateRecyclerView() {
        final YoutubeVideoAdapter adapter = new YoutubeVideoAdapter(this, youtubeVideoArrayList);
        mBinding.recyclerView.setAdapter(adapter);

        //set click event
        mBinding.recyclerView.addOnItemTouchListener(new RecyclerViewOnClickListener(this, new RecyclerViewOnClickListener.OnItemClickListener() {
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
            mBinding.ivFavButton.setImageResource(R.drawable.ic_favorite_solid_24dp);
            insertFilmInDatabase(film,context);
        } else {
            if (isBrightMood){
                mBinding.ivFavButton.setImageResource(R.drawable.ic_favorite_border_black_24dp);
            }else {
                mBinding.ivFavButton.setImageResource(R.drawable.ic_favorite_border_white_24dp);
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
        mBinding.loadingPointer.setVisibility(View.GONE);
        if (passed != null) {
            isDataLoaded = true;
            if (passed.JSONData != null && passed.JSONData.size() != 0) {
                mBinding.recyclerView.setVisibility(View.VISIBLE);
                mBinding.emptyView.setVisibility(View.GONE);
                youtubeVideoArrayList = passed.JSONData;
                initializeYoutubePlayer();
                populateRecyclerView();
            } else {
                mBinding.recyclerView.setVisibility(View.GONE);
                mBinding.emptyView.setVisibility(View.VISIBLE);

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
            mBinding.ivFavButton.setImageResource(R.drawable.ic_favorite_solid_24dp);
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

        if (isConnected && !whenAppLaunchFirstTime) {
            mBinding.internetSituation.setVisibility(View.VISIBLE);
            mBinding.internetSituation.setText(R.string.back_online);
            mBinding.internetSituation.setBackgroundColor(getResources().getColor(R.color.online));
            CountDownTimer timer = new CountDownTimer(5000, 5000) {
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    mBinding.internetSituation.setVisibility(View.GONE);
                }
            };
            timer.start();

            if (!isDataLoaded) {
                LoaderManager loaderManager = LoaderManager.getInstance(this);
                mBinding.loadingPointer.setVisibility(View.VISIBLE);
                mBinding.emptyView.setVisibility(View.GONE);
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
                        .into(mBinding.filmPoster);
                Picasso.with(this)
                        .load(backdropUrl)
                        .into(mBinding.filmBackdrop);
                isImageLoaded = true;
            }
        } else if (!isConnected) {
            mBinding.internetSituation.setVisibility(View.VISIBLE);
            mBinding.internetSituation.setText(R.string.offline_message);
            mBinding.internetSituation.setBackgroundColor(getResources().getColor(R.color.cardview_dark_background));
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