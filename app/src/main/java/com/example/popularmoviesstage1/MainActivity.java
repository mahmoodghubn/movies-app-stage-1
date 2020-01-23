package com.example.popularmoviesstage1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.databinding.DataBindingUtil;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.SearchRecentSuggestions;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.popularmoviesstage1.Data.MySuggestionProvider;
import com.example.popularmoviesstage1.databinding.ActivityMainBinding;
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
        , LoaderManager.LoaderCallbacks<ArrayList<Film>>, SharedPreferences.OnSharedPreferenceChangeListener {
    //TODO  Add visual polish and styling to your app, including custom colors, fonts and styles, accounting for multiple devices
    //TODO Try different views, viewgroups and alternative layouts, perform data binding, make your app accessible
    //TODO adding animation
    //TODO adding comments
    //TODO beautifying decorate the page buttons
    public static String SHARED_ELEMENT_TRANSITION_EXTRA = "sharedElementTransition";

    ActivityMainBinding mBinding;
    private FilmAdapter mAdapter;
    public static PageNumber pageNumber;
    IntentFilter internetConnectionIntentFilter;
    InternetBroadCastReceiver internetBroadCastReceiver;
    boolean isDataLoaded;
    //this boolean is to forbid the back online statement from appearing when app first launch and when resuming
    boolean whenAppLaunchFirstTime = true;
    private String searchQuery;
    boolean inPreferences;
    static boolean isBrightMood;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    ProgressBar loadingIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        isBrightMood = sharedPreferences.getBoolean("bright", false);
        if (isBrightMood) {
            setTheme(R.style.BrightTheme);
        }
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        loadingIndicator = findViewById(R.id.loading_indicator);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        pageNumber = new PageNumber(null, null);
        editor = sharedPreferences.edit();
        inPreferences = sharedPreferences.getBoolean("home_page", true);
        if (inPreferences) {
            PageNumber.setPopularity_page_number(sharedPreferences.getInt("popular", 1));
            PageNumber.setHigh_rated_page_number(sharedPreferences.getInt("top_rated", 1));
            PageNumber.setFavorite_page_number(sharedPreferences.getInt("favorite", 1));
            PageNumber.setSearch_page_number(sharedPreferences.getInt("search", 1));
            PageNumber.setPageSort(sharedPreferences.getString("page_sort", "POPULARITY"));
            searchQuery = sharedPreferences.getString("search_query", "");
            mBinding.pageNumber.setText(String.valueOf(pageNumber.getCurrentPageNum()));
            if (pageNumber.getCurrentPageSort().equals("FAVORITE"))
                setVisibility(View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);

        }
        //at first we will start with page number one and popularity type
        //restoring data after rotation
        if (savedInstanceState != null) {
            pageNumber = (PageNumber) savedInstanceState.getSerializable("page_number");
            mBinding.pageNumber.setText(String.valueOf(pageNumber.getCurrentPageNum()));
        }



        RecyclerView.LayoutManager gridLayoutManager = new GridLayoutManager(getApplicationContext(), 2);
        mBinding.recyclerView.setLayoutManager(gridLayoutManager);
        mBinding.recyclerView.setHasFixedSize(true);
        mAdapter = new FilmAdapter(this);
        //the app becomes life cycle aware
        LoaderManager loaderManager = LoaderManager.getInstance(this);
        //the id of the loader is the same as page number
        int PNom = pageNumber.getCurrentPageNum();
        Loader<ArrayList<Film>> loader = loaderManager.getLoader(PNom);
        if (loader == null) {
            loaderManager.initLoader(PNom, null, this);
        } else {
            loaderManager.restartLoader(PNom, null, this);
        }
        mBinding.recyclerView.setAdapter(mAdapter);



        mBinding.icRight.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onClick(View view) {
                int previousPN = Integer.parseInt(mBinding.pageNumber.getText().toString());
                mBinding.pageNumber.setText(String.valueOf(++previousPN));
                //calculating the new page number
                pageNumber = new PageNumber(null, previousPN);
                //getting the current page
                LoaderManager loaderManager = LoaderManager.getInstance(MainActivity.this);
                int PNom = pageNumber.getCurrentPageNum();
                Loader<ArrayList<Film>> loader = loaderManager.getLoader(PNom);
                setVisibility(View.VISIBLE, View.GONE, View.GONE, null, null, null);
                if (loader == null) {
                    loaderManager.initLoader(PNom, null, MainActivity.this);
                } else {
                    loaderManager.restartLoader(PNom, null, MainActivity.this);
                }
            }
        });
        mBinding.icLeft.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onClick(View view) {
                int previousPN = Integer.parseInt(mBinding.pageNumber.getText().toString());
                if (previousPN != 1) {
                    mBinding.pageNumber.setText(String.valueOf(--previousPN));
                    pageNumber = new PageNumber(null, previousPN);
                    LoaderManager loaderManager = LoaderManager.getInstance(MainActivity.this);
                    int PNom = pageNumber.getCurrentPageNum();
                    Loader<ArrayList<Film>> loader = loaderManager.getLoader(PNom);
                    //this line of code because the loading indicator will be set to gone after load is finish
                    setVisibility(View.VISIBLE, View.GONE, View.GONE, null, null, null);
                    if (loader == null) {
                        loaderManager.initLoader(PNom, null, MainActivity.this);
                    } else {
                        loaderManager.restartLoader(PNom, null, MainActivity.this);
                    }
                }
            }
        });
        internetConnectionIntentFilter = new IntentFilter();
        internetConnectionIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        internetBroadCastReceiver = new InternetBroadCastReceiver();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    private void setVisibility(@Nullable Integer loadingIndicator, @Nullable Integer mRecyclerView, @Nullable Integer emptyView, @Nullable Integer previousPage, @Nullable Integer thisPage, @Nullable Integer nextPage) {
        if (loadingIndicator != null)
            this.loadingIndicator.setVisibility(loadingIndicator);
        if (mRecyclerView != null)
            this.mBinding.recyclerView.setVisibility(mRecyclerView);
        if (emptyView != null)
            this.mBinding.emptyView.setVisibility(emptyView);
        if (previousPage != null)
            this.mBinding.icLeft.setVisibility(previousPage);
        if (nextPage != null)
            this.mBinding.icRight.setVisibility(nextPage);
        if (thisPage != null)
            this.mBinding.pageNumber.setVisibility(thisPage);
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
        if (inPreferences) {
            editor.putInt("popular", PageNumber.getPopularity_page_number());
            editor.putInt("top_rated", PageNumber.getHigh_rated_page_number());
            editor.putInt("favorite", PageNumber.getFavorite_page_number());
            editor.putInt("search", PageNumber.getSearch_page_number());
            editor.putString("page_sort", pageNumber.getCurrentPageSort());
            editor.putString("search_query", searchQuery);
            editor.commit();
        }
    }

    @Override
    public void onClickItem(Film oneFilmData, View view) {
        Intent intent = new Intent(MainActivity.this, DetailActivity.class);
        intent.putExtra("FilmClass", oneFilmData);
        if (view.getId() == R.id.iv_item)
            Log.i(view.getId() + "", "dddd");
        else
            Log.i(view.getId() + "", "ddddsss");

        @SuppressWarnings("unchecked")
        ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                MainActivity.this,
                view.findViewById(R.id.iv_item),
                SHARED_ELEMENT_TRANSITION_EXTRA);

        startActivity(intent, optionsCompat.toBundle());
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
                    URL filmsRequestUrl;
                    if (!pageNumber.getCurrentPageSort().equals("SEARCH")) {
                        filmsRequestUrl = NetworkUtils.createUrl(MainActivity.this, String.valueOf(pageNumber.getCurrentPageNum()), pageNumber.getCurrentPageSort());
                    } else {
                        filmsRequestUrl = NetworkUtils.createSearchUrl(MainActivity.this, String.valueOf(pageNumber.getCurrentPageNum()), searchQuery);
                    }
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
                            FilmEntry.COLUMN_VOTE_AVERAGE, FilmEntry.COLUMN_POSTER, FilmEntry.COLUMN_OVERVIEW, FilmEntry.COLUMN_BACKDROP_PATH};

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
                        int backdropPathColumnIndex = cursor.getColumnIndex(FilmEntry.COLUMN_BACKDROP_PATH);

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
                            String currentBackdropPath = cursor.getString(backdropPathColumnIndex);
                            favFilm.add(new Film(currentPoster, currentFilmTitle, currentOverview, currentFilmDate, currentAverage, "" + currentID, currentBackdropPath));
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

        if (data != null) {
            setVisibility(View.GONE, View.VISIBLE, View.GONE, null, null, null);
            isDataLoaded = true;
            mAdapter.setFilmData(data);
        } else {
            setVisibility(View.GONE, View.GONE, View.VISIBLE, null, null, null);
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
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            doMySearch(query);
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    MySuggestionProvider.AUTHORITY, MySuggestionProvider.MODE);
            suggestions.saveRecentQuery(query, null);
        }
    }

    private void doMySearch(String query) {
        setVisibility(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.VISIBLE, View.VISIBLE);
        mAdapter.setFilmData(null);
        pageNumber = new PageNumber(PageType.SEARCH, 1);
        searchQuery = query;
        int PNom = pageNumber.getCurrentPageNum();
        LoaderManager loaderManager = LoaderManager.getInstance(this);
        Loader<ArrayList<Film>> loader = loaderManager.getLoader(PNom);
        if (loader == null) {
            loaderManager.initLoader(PNom, null, this);
        } else {
            loaderManager.restartLoader(PNom, null, this);
        }
        mBinding.pageNumber.setText(String.valueOf(PNom));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        //saving the current page number after rotation
        outState.putSerializable("page_number", pageNumber);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.most_popular && !pageNumber.getCurrentPageSort().equals(NetworkUtils.POPULARITY)) {
            setVisibility(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.VISIBLE, View.VISIBLE);
            mAdapter.setFilmData(null);
            pageNumber = new PageNumber(PageType.POPULARITY, null);
            int PNom = pageNumber.getCurrentPageNum();
            LoaderManager loaderManager = LoaderManager.getInstance(this);
            Loader<ArrayList<Film>> loader = loaderManager.getLoader(PNom);
            if (loader == null) {
                loaderManager.initLoader(PNom, null, this);
            } else {
                loaderManager.restartLoader(PNom, null, this);
            }
            mBinding.pageNumber.setText(String.valueOf(PNom));
            return true;
        } else if (id == R.id.highest_rated && !pageNumber.getCurrentPageSort().equals(NetworkUtils.HIGHEST_RATED)) {
            setVisibility(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.VISIBLE, View.VISIBLE);
            mAdapter.setFilmData(null);
            pageNumber = new PageNumber(PageType.TOP_RATED, null);
            int PNom = pageNumber.getCurrentPageNum();

            LoaderManager loaderManager = LoaderManager.getInstance(this);
            Loader<ArrayList<Film>> loader = loaderManager.getLoader(PNom);
            if (loader == null) {
                loaderManager.initLoader(PNom, null, this);
            } else {
                loaderManager.restartLoader(PNom, null, this);
            }
            mBinding.pageNumber.setText(String.valueOf(PNom));
            return true;
        } else if (id == R.id.favorite && !pageNumber.getCurrentPageSort().equals("FAVORITE")) {
            setVisibility(View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
            mAdapter.setFilmData(null);
            pageNumber = new PageNumber(PageType.FAVORITE, null);
            int PNom = pageNumber.getCurrentPageNum();
            LoaderManager loaderManager = LoaderManager.getInstance(this);
            Loader<ArrayList<Film>> loader = loaderManager.getLoader(PNom);
            if (loader == null) {
                loaderManager.initLoader(PNom, null, this);
            } else {
                loaderManager.restartLoader(PNom, null, this);
            }
            mBinding.pageNumber.setText(String.valueOf(PNom));

        } else if (id == R.id.delete_history) {
            //deleting the history of the search
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    MySuggestionProvider.AUTHORITY, MySuggestionProvider.MODE);
            suggestions.clearHistory();
        } else if (id == R.id.settings) {
            Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
            startActivity(startSettingsActivity);
            return true;
        }
        return super.onOptionsItemSelected(item);
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
                //the id of the loader is the same as page number
                int PNom = pageNumber.getCurrentPageNum();
                Loader<ArrayList<Film>> loader = loaderManager.getLoader(PNom);
                setVisibility(View.VISIBLE, null, View.GONE, null, null, null);

                if (loader == null) {
                    loaderManager.initLoader(PNom, null, this);
                } else {
                    loaderManager.restartLoader(PNom, null, this);
                }
            }

        } else if (!isConnected) {
            online_situation.setVisibility(View.VISIBLE);
            online_situation.setText(R.string.offline_message);
            online_situation.setBackgroundColor(getResources().getColor(R.color.cardview_dark_background));
        }
        whenAppLaunchFirstTime = false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("bright")) {
            if (sharedPreferences.getBoolean("bright", false)) {
                setTheme(R.style.AppTheme);
            } else {
                setTheme(R.style.BrightTheme);
            }
            recreate();

        }
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