package com.example.popularmoviesstage1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.popularmoviesstage1.model.Film;
import com.example.popularmoviesstage1.model.PageNumber;
import com.example.popularmoviesstage1.model.PageNumber.*;
import com.example.popularmoviesstage1.utilities.NetworkUtils;

import java.net.URL;
import java.util.ArrayList;

import com.example.popularmoviesstage1.FilmAdapter.FilmAdapterOnClickHandler;
import com.example.popularmoviesstage1.Data.FilmContract.*;

import static android.os.SystemClock.sleep;
import static com.example.popularmoviesstage1.Data.FilmContract.FilmEntry.CONTENT_URI;

public class MainActivity extends AppCompatActivity implements FilmAdapterOnClickHandler
        , LoaderManager.LoaderCallbacks<ArrayList<Film>> {
    //TODO beautifying decorate the page buttons
    //TODO change the layout of landscape mode for the main view
    //TODO beautifying the detail activity
    //TODO adding animation
    //TODO adding search button
    //TODO adding notification if possible
    //TODO adding preferences which page to start off ,the number of movies per page
    //TODO  Add visual polish and styling to your app, including custom colors, fonts and styles, accounting for multiple devices
    //TODO Try different views, viewgroups and alternative layouts, perform data binding, make your app accessible
    //TODO adding comments
    /*Suggestions to Make Your Project Stand Out!
    Extend the favorites database to store the movie poster, synopsis, user rating, and release date, and display them even when offline.
    Implement sharing functionality to allow the user to share the first trailer’s YouTube URL from the movie details screen.
    */
    ImageButton nextPage;
    ImageButton previousPage;
    TextView thisPage;
    private FilmAdapter mAdapter;
    public static PageNumber pageNumber;
    //the sort of the current page
    private String sort = NetworkUtils.POPULARITY;
    IntentFilter internetConnectionIntentFilter;
    InternetBroadCastReceiver internetBroadCastReceiver;
    boolean isDataLoaded;
    private TextView emptyView;
    RecyclerView mRecyclerView;
    View loadingIndicator;
    Bundle filmBundle;
    //this boolean is to forbid the back online statement from appearing when app first launch and when resuming
    boolean whenAppLaunchFirstTime = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        emptyView = findViewById(R.id.empty_view);
        loadingIndicator = findViewById(R.id.loading_indicator);
        thisPage = findViewById(R.id.page_number);
        //at first we will start with page number one and popularity type
        pageNumber = new PageNumber(null, null);
        //restoring data after rotation
        if (savedInstanceState != null) {
            pageNumber = (PageNumber) savedInstanceState.getSerializable("pageNumber");
            assert pageNumber != null;
            thisPage.setText(String.valueOf(pageNumber.getCurrentPageNumber()));
        }
        //the arrows for the previous and next page
        previousPage = findViewById(R.id.ic_left);
        nextPage = findViewById(R.id.ic_right);
        mRecyclerView = findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager gridLayoutManager = new GridLayoutManager(getApplicationContext(), 2);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new FilmAdapter(this);
        //saving data for rotation
        filmBundle = new Bundle();
        filmBundle.putSerializable("pageNumber", pageNumber);
        //the app becomes life cycle aware
        LoaderManager loaderManager = LoaderManager.getInstance(this);
        //the id of the loader is the same as page number
        Loader<ArrayList<Film>> loader = loaderManager.getLoader(pageNumber.getCurrentPageNumber());
        if (loader == null) {
            loaderManager.initLoader(pageNumber.getCurrentPageNumber(), filmBundle, this);
        } else {
            loaderManager.restartLoader(pageNumber.getCurrentPageNumber(), filmBundle, this);
        }
        mRecyclerView.setAdapter(mAdapter);
        nextPage.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onClick(View view) {
                int previousPageNumber = Integer.parseInt(thisPage.getText().toString());
                thisPage.setText(String.valueOf( ++previousPageNumber));
                Bundle filmBundle = new Bundle();
                //calculating the new page number
                pageNumber = new PageNumber(null, previousPageNumber);
                //saving the current page number in case of rotation
                filmBundle.putSerializable("pageNumber", pageNumber);
                filmBundle.putString("sortType", sort);
                //getting the current page
                LoaderManager loaderManager = LoaderManager.getInstance(MainActivity.this);
                Loader<ArrayList<Film>> loader = loaderManager.getLoader(pageNumber.getCurrentPageNumber());
                loadingIndicator.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.GONE);
                if (loader == null) {
                    loaderManager.initLoader(pageNumber.getCurrentPageNumber(), filmBundle, MainActivity.this);
                } else {
                    loaderManager.restartLoader(pageNumber.getCurrentPageNumber(), filmBundle, MainActivity.this);
                }
            }
        });
        previousPage.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onClick(View view) {
                int previousPageNumber = Integer.parseInt(thisPage.getText().toString());
                if (previousPageNumber != 1)
                    thisPage.setText(String.valueOf( --previousPageNumber));
                pageNumber = new PageNumber(null, previousPageNumber);
                Bundle filmBundle = new Bundle();
                filmBundle.putSerializable("pageNumber", pageNumber);
                LoaderManager loaderManager = LoaderManager.getInstance(MainActivity.this);
                Loader<ArrayList<Film>> loader = loaderManager.getLoader(pageNumber.getCurrentPageNumber());
                //this line of code because the loading indicator will be set to gone after load is finish
                loadingIndicator.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.GONE);
                if (loader == null) {
                    loaderManager.initLoader(pageNumber.getCurrentPageNumber(), filmBundle, MainActivity.this);
                } else {
                    loaderManager.restartLoader(pageNumber.getCurrentPageNumber(), filmBundle, MainActivity.this);
                }
            }
        });
        internetConnectionIntentFilter = new IntentFilter();
        internetConnectionIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        internetBroadCastReceiver = new InternetBroadCastReceiver();
    }

    @Override
    protected void onResume(){
        super.onResume();
        registerReceiver(internetBroadCastReceiver,internetConnectionIntentFilter);
    }

    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(internetBroadCastReceiver);
        whenAppLaunchFirstTime= true;
    }

    @Override
    public void onClick(Film oneFilmData) {
        Intent intent = new Intent(MainActivity.this, DetailActivity.class);
        intent.putExtra("FilmClass", oneFilmData);
        MainActivity.this.startActivity(intent);
    }

    @NonNull
    @Override
    public Loader<ArrayList<Film>> onCreateLoader(int id, @Nullable final Bundle args) {
        if (!pageNumber.getCurrentPageSort().equals("FAVORITE")) {
            return new AsyncTaskLoader<ArrayList<Film>>(this) {
                @Override
                protected void onStartLoading() {
                    super.onStartLoading();
                    forceLoad();
                }

                @Nullable
                @Override
                public ArrayList<Film> loadInBackground() {
                    //getting the films of the new page
                    assert args != null;
                    PageNumber pageNumber = (PageNumber) args.getSerializable("pageNumber");
                    assert pageNumber != null;
                    URL filmsRequestUrl = NetworkUtils.createUrl(MainActivity.this, String.valueOf(pageNumber.getCurrentPageNumber()), pageNumber.getCurrentPageSort());

                    try {
                        String jsonFilmsResponse = NetworkUtils
                                .getResponseFromHttpUrl(filmsRequestUrl);
                        return NetworkUtils
                                .extractFeatureFromJson(MainActivity.this, jsonFilmsResponse);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            };
        } else {

            return new AsyncTaskLoader<ArrayList<Film>>(this) {
                @Override
                protected void onStartLoading() {
                    super.onStartLoading();
                    forceLoad();
                }

                @NonNull
                @Override
                public ArrayList<Film> loadInBackground() {
                    String[] projection = {FilmEntry._ID, FilmEntry.COLUMN_FILM_TITLE, FilmEntry.COLUMN_DATE,
                            FilmEntry.COLUMN_VOTE_AVERAGE, FilmEntry.COLUMN_POSTER, FilmEntry.COLUMN_OVERVIEW};

                    Cursor cursor = getContentResolver().query(CONTENT_URI, projection, null, null,
                            null, null);
                    ArrayList<Film> favFilm = new ArrayList<>();

                    try {
                        // Figure out the index of each column
                        assert cursor != null;
                        int idColumnIndex = cursor.getColumnIndex(FilmEntry._ID);
                        int nameColumnIndex = cursor.getColumnIndex(FilmEntry.COLUMN_FILM_TITLE);
                        int dateColumnIndex = cursor.getColumnIndex(FilmEntry.COLUMN_DATE);
                        int averageColumnIndex = cursor.getColumnIndex(FilmEntry.COLUMN_VOTE_AVERAGE);
                        int posterColumnIndex = cursor.getColumnIndex(FilmEntry.COLUMN_POSTER);
                        int overviewColumnIndex = cursor.getColumnIndex(FilmEntry.COLUMN_OVERVIEW);

                        // Iterate through all the returned rows in the cursor
                        while (cursor.moveToNext()) {
                            // Use that index to extract the String or Int value of the word
                            // at the current row the cursor is on.
                            int currentID = cursor.getInt(idColumnIndex);
                            String currentFilmTitle = cursor.getString(nameColumnIndex);
                            String currentFilmDate = cursor.getString(dateColumnIndex);
                            String currentAverage = cursor.getString(averageColumnIndex);
                            String currentPoster = cursor.getString(posterColumnIndex);
                            String currentOverview = cursor.getString(overviewColumnIndex);
                            favFilm.add(new Film(currentPoster, currentFilmTitle, currentOverview, currentFilmDate, currentAverage, "" + currentID));
                        }
                    } finally {
                        // Always close the cursor when you're done reading from it. This releases all its
                        // resources and makes it invalid.
                        assert cursor != null;
                        cursor.close();
                    }
                    return favFilm;
                }
            };
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<ArrayList<Film>> loader, ArrayList<Film> data) {
        loadingIndicator.setVisibility(View.GONE);

        if (data != null) {
            mRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            isDataLoaded =true;
            mAdapter.setFilmData(data);
        } else {
            mRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            isDataLoaded = false;
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<ArrayList<Film>> loader) {
        //preventing memory leaks by using this call
        mAdapter.setFilmData(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Use AppCompatActivity's method getMenuInflater to get a handle on the menu inflater */
        MenuInflater inflater = getMenuInflater();
        /* Use the inflater's inflate method to inflate our menu layout to this menu */
        inflater.inflate(R.menu.menu, menu);
        /* Return true so that the menu is displayed in the Toolbar */
        return true;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        //saving the current page number after rotation
        outState.putSerializable("pageNumber", pageNumber);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.most_popular && !pageNumber.getCurrentPageSort().equals(NetworkUtils.POPULARITY)) {
            loadingIndicator.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
            nextPage.setVisibility(View.VISIBLE);
            previousPage.setVisibility(View.VISIBLE);
            thisPage.setVisibility(View.VISIBLE);
            mAdapter.setFilmData(null);
            pageNumber = new PageNumber(PageType.POPULARITY, null);
            filmBundle = new Bundle();
            filmBundle.putSerializable("pageNumber", pageNumber);
            LoaderManager loaderManager = LoaderManager.getInstance(this);
            Loader<ArrayList<Film>> loader = loaderManager.getLoader(pageNumber.getCurrentPageNumber());
            if (loader == null) {
                loaderManager.initLoader(pageNumber.getCurrentPageNumber(), filmBundle, this);
            } else {
                loaderManager.restartLoader(pageNumber.getCurrentPageNumber(), filmBundle, this);
            }
            thisPage.setText(String.valueOf(pageNumber.getCurrentPageNumber()));
            return true;
        } else if (id == R.id.highest_rated && !pageNumber.getCurrentPageSort().equals(NetworkUtils.HIGHEST_RATED)) {
            loadingIndicator.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
            nextPage.setVisibility(View.VISIBLE);
            previousPage.setVisibility(View.VISIBLE);
            thisPage.setVisibility(View.VISIBLE);
            mAdapter.setFilmData(null);
            pageNumber = new PageNumber(PageType.HIGH_RATED, null);
            Bundle filmBundle = new Bundle();
            filmBundle.putSerializable("pageNumber", pageNumber);
            LoaderManager loaderManager = LoaderManager.getInstance(this);
            Loader<ArrayList<Film>> loader = loaderManager.getLoader(pageNumber.getCurrentPageNumber());
            if (loader == null) {
                loaderManager.initLoader(pageNumber.getCurrentPageNumber(), filmBundle, this);
            } else {
                loaderManager.restartLoader(pageNumber.getCurrentPageNumber(), filmBundle, this);
            }
            thisPage.setText(String.valueOf(pageNumber.getCurrentPageNumber()));
            return true;
        } else if (id == R.id.favorite && !pageNumber.getCurrentPageSort().equals("FAVORITE")) {
            loadingIndicator.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
            nextPage.setVisibility(View.GONE);
            previousPage.setVisibility(View.GONE);
            thisPage.setVisibility(View.GONE);
            mAdapter.setFilmData(null);
            pageNumber = new PageNumber(PageType.FAVORITE, null);
            Bundle filmBundle = new Bundle();
            filmBundle.putSerializable("pageNumber", pageNumber);
            LoaderManager loaderManager = LoaderManager.getInstance(this);
            Loader<ArrayList<Film>> loader = loaderManager.getLoader(pageNumber.getCurrentPageNumber());
            if (loader == null) {
                loaderManager.initLoader(pageNumber.getCurrentPageNumber(), filmBundle, this);
            } else {
                loaderManager.restartLoader(pageNumber.getCurrentPageNumber(), filmBundle, this);
            }
            thisPage.setText(String.valueOf(pageNumber.getCurrentPageNumber()));

        }
        return super.onOptionsItemSelected(item);
    }

    public void showInternetConnection(boolean isConnected) {
        final TextView online_situation = findViewById(R.id.internet_situation);
        if (isConnected && !whenAppLaunchFirstTime) {
            //Toast.makeText(this,"Back online",Toast.LENGTH_LONG).show();
            online_situation.setVisibility(View.VISIBLE);
            online_situation.setText(R.string.back_online);
            online_situation.setBackgroundColor(getResources().getColor(R.color.online));
            CountDownTimer timer = new CountDownTimer(5000, 5000)
            {
                public void onTick(long millisUntilFinished)
                {
                }

                public void onFinish()
                {
                    online_situation.setVisibility(View.GONE);
                }
            };
            timer.start();
            if (!isDataLoaded ){
                Bundle filmBundle = new Bundle();
                filmBundle.putSerializable("pageNumber", pageNumber);
                LoaderManager loaderManager = LoaderManager.getInstance(this);
                //the id of the loader is the same as page number
                Loader<ArrayList<Film>> loader = loaderManager.getLoader(pageNumber.getCurrentPageNumber());
                loadingIndicator.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                if (loader == null) {
                    loaderManager.initLoader(pageNumber.getCurrentPageNumber(), filmBundle, this);
                } else {
                    loaderManager.restartLoader(pageNumber.getCurrentPageNumber(), filmBundle, this);
                }
            }

        } else if(!isConnected){
            online_situation.setVisibility(View.VISIBLE);
            online_situation.setText(R.string.offline_message);
            online_situation.setBackgroundColor(getResources().getColor(R.color.cardview_dark_background));
        }
        whenAppLaunchFirstTime =false;
    }

    private class InternetBroadCastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            assert action != null;
            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                boolean isConnected = info.isConnected();
                sleep(1000);
                showInternetConnection(isConnected);
            }
        }
    }
}