package com.example.popularmoviesstage1;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
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

import com.example.popularmoviesstage1.model.Film;
import com.example.popularmoviesstage1.utilities.NetworkUtils;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.squareup.picasso.Picasso;
import com.example.popularmoviesstage1.Data.FilmContract.FilmEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import com.example.popularmoviesstage1.ReviewAdapter.ReviewAdapterOnClickHandler;
import com.squareup.picasso.Target;
import static android.os.SystemClock.sleep;
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
    String poster;
    String posterURI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        emptyView = findViewById(R.id.empty_view);
        loadingIndicator = findViewById(R.id.loading_indicator);
        context = getBaseContext();
        favoriteFilmButton = findViewById(R.id.iv_favButton);
        imageView = findViewById(R.id.film_image);
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
        poster = film.getPoster();
        posterURI = film.getPosterURI();
        filmUrl = NetworkUtils.buildPosterUrl(poster, NetworkUtils.ORIGINAL);
        title = findViewById(R.id.film_title);
        title.setText(film.getTitle());
        voteAverage = findViewById(R.id.vote_average);
        voteAverage.setText(film.getVoteAverage());
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
                    youTubePlayer.loadVideos(youtubeVideoArrayList);
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
            insertFilmInDatabase(film, context);
        } else {
            favoriteFilmButton.setImageResource(R.drawable.ic_favorite_border_black_24dp);
            deleteFilmFromDatabase(film, context);
        }
        insideDB = !insideDB;
    }

    public boolean queryFilmFromDatabase(String filmId, Context context) {
        String[] projection = {FilmEntry._ID};
        Cursor cursor = context.getContentResolver().query(CONTENT_URI.buildUpon().appendPath(filmId).build(), projection, _ID + "=?", new String[]{filmId}, null);
        assert cursor != null;
        boolean isInside = cursor.moveToFirst();
        cursor.close();
        return isInside;
    }

    public Cursor gainCursorFromDatabase(String filmId, Context context) {
        String[] projection = {FilmEntry._ID, COLUMN_POSTER_URI};
        return context.getContentResolver().query(CONTENT_URI.buildUpon().appendPath(filmId).build(), projection, _ID + "=?", new String[]{filmId}, null);
    }

    public void deleteFilmFromDatabase(Film film, Context context) {
        int deletedRows = context.getContentResolver().delete(CONTENT_URI, _ID + "=?", new String[]{film.getId()});
        ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File myPath = new File(directory, film.getTitle());
        myPath.delete();
        if (deletedRows == 0) {
            // If the new content URI is null, then there was an error with insertion.
            Toast.makeText(context, context.getString(R.string.editor_delete_film_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, context.getString(R.string.editor_delete_film_successful),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void insertFilmInDatabase(Film film, Context context) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(_ID, Integer.parseInt(film.getId()));
        contentValues.put(COLUMN_FILM_TITLE, film.getTitle());
        contentValues.put(COLUMN_DATE, film.getReleaseDate());
        contentValues.put(COLUMN_VOTE_AVERAGE, film.getVoteAverage());
        contentValues.put(COLUMN_POSTER, film.getPoster());
        filmUrl = NetworkUtils.buildPosterUrl(film.getPoster(), NetworkUtils.ORIGINAL);
        //
        /*ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        contentValues.put(COLUMN_POSTER_URI, new File(directory, film.getTitle()).getAbsolutePath());
        Uri newUri = context.getContentResolver().insert(CONTENT_URI, contentValues);

        Picasso.with(this)//trying to download and save the poster again
                .load(filmUrl)
                .into(getTarget(context,film.getTitle()));
        if (newUri == null) {
            // If the new content URI is null, then there was an error with insertion.
            Toast.makeText(context, context.getString(R.string.editor_insert_film_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, context.getString(R.string.editor_insert_film_successful),
                    Toast.LENGTH_SHORT).show();
        }*/
        DownloadImageTask downloadImageTask = new DownloadImageTask(contentValues, film, context);
        downloadImageTask.execute(filmUrl);
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
                boolean hasObject = queryFilmFromDatabase(filmId, context);
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
            isImageLoaded = true;
            if (!loadImageFromStorage(posterURI, imageView)) {//if the image is not saved in the storage
                Picasso.with(this)//trying to download and save the poster again
                        .load(filmUrl)
                        .into(getTarget(context, film.getTitle()));
                loadImageFromStorage(posterURI, imageView);
            }
        } else {
            ConnectivityManager connMgr = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            // Get details on the currently active default data network
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            // If there is a network connection, fetch data
            if (networkInfo != null && networkInfo.isConnected()) {
                Picasso.with(this)
                        .load(filmUrl)
                        .into(imageView);
                isImageLoaded = true;

            }
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
                        .into(imageView);
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

    boolean loadImageFromStorage(String path, ImageView imageView) {

        Bitmap b = null;
        try {
            File f = new File(path);
            b = BitmapFactory.decodeStream(new FileInputStream(f));
            imageView.setImageBitmap(b);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return b != null;
    }

    private static String saveToInternalStorage(Bitmap bitmapImage, String fileName, Context context) {
        ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File myPath = new File(directory, fileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(myPath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return myPath.getAbsolutePath();
    }


    private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ContentValues contentValues;
        Film film;
        Context context;
        DownloadImageTask(ContentValues contentValues, Film film, Context context) {
            this.contentValues = contentValues;
            this.film = film;
            this.context = context;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            contentValues.put(COLUMN_POSTER_URI, saveToInternalStorage(result, film.getTitle(), context));
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
    }

    //target to save
    public static Target getTarget(final Context context, final String fileName) {
        Target target = new Target() {

            @Override
            public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        ContextWrapper cw = new ContextWrapper(context);
                        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                        // Create imageDir
                        File myPath = new File(directory, fileName);
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(myPath);
                            // Use the compress method on the BitMap object to write image to the OutputStream
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        };
        return target;
    }
}