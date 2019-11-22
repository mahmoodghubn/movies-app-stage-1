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
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
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

import static com.example.popularmoviesstage1.Data.FilmContract.FilmEntry.CONTENT_URI;

public class MainActivity extends AppCompatActivity implements FilmAdapterOnClickHandler
        , LoaderManager.LoaderCallbacks<ArrayList<Film>> {
    //TODO beautifying decorate the page buttons
    //TODO change the layout of landscape mode for the main view
    //TODO beautifying the detail activity
    //TODO fixing the rotation of the detail activity to start of the part that
    //TODO fixing next and previous page button
    /*The titles and IDs of the user’s favorite movies are stored in a native SQLite database and exposed via a ContentProvider
    OR
    stored using Room.
    Data is updated whenever the user favorites or unfavorites a movie. No other persistence libraries are used.*/

    /*When the "favorites" setting option is selected, the main view displays the entire favorites collection based on movie ids stored in the database.
     */

    /*If Room is used, database is not re-queried unnecessarily. LiveData is used to observe changes in the database and update the UI accordingly.
     */

    /* If Room is used, database is not re-queried unnecessarily after rotation. Cached LiveData from ViewModel is used instead.
     */

    /*Suggestions to Make Your Project Stand Out!
    Extend the favorites database to store the movie poster, synopsis, user rating, and release date, and display them even when offline.
    Implement sharing functionality to allow the user to share the first trailer’s YouTube URL from the movie details screen.
    */
    ImageButton nextPage;
    ImageButton previousPage;
    TextView thisPage;

    private FilmAdapter mAdapter;
    private PageNumber pageNumber;
    //the sort of the current page
    private String sort = NetworkUtils.POPULARITY;
    public static LoaderManager loaderManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        thisPage = findViewById(R.id.page_number);
        //at first we will start with page number one and popularity type
        pageNumber = new PageNumber(null, null);

        //restoring data after rotation
        if (savedInstanceState != null) {
            pageNumber = (PageNumber) savedInstanceState.getSerializable("pageNumber");
            thisPage.setText("" + pageNumber.getCurrentPageNumber());
        }
        //the arrows for the previous and next page
        previousPage = findViewById(R.id.ic_left);
        nextPage = findViewById(R.id.ic_right);

        RecyclerView mRecyclerView = findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager gridLayoutManager = new GridLayoutManager(getApplicationContext(), 2);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new FilmAdapter(this);

        //saving data for rotation
        Bundle filmBundle = new Bundle();
        filmBundle.putSerializable("pageNumber", pageNumber);
        //the app becomes life cycle aware
        loaderManager = LoaderManager.getInstance(this);
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
                thisPage.setText(String.format("%d", ++previousPageNumber));
                Bundle filmBundle = new Bundle();
                //calculating the new page number
                pageNumber = new PageNumber(null, previousPageNumber);

                //saving the current page number in case of rotation
                filmBundle.putSerializable("pageNumber", pageNumber);
                filmBundle.putString("sortType", sort);
                //getting the current page
                LoaderManager loaderManager = LoaderManager.getInstance(MainActivity.this);
                Loader<ArrayList<Film>> loader = loaderManager.getLoader(pageNumber.getCurrentPageNumber());
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
                    thisPage.setText(String.format("%d", --previousPageNumber));
                pageNumber = new PageNumber(null, previousPageNumber);
                Bundle filmBundle = new Bundle();
                filmBundle.putSerializable("pageNumber", pageNumber);
                LoaderManager loaderManager = LoaderManager.getInstance(MainActivity.this);
                Loader<ArrayList<Film>> loader = loaderManager.getLoader(pageNumber.getCurrentPageNumber());
                if (loader == null) {
                    loaderManager.initLoader(pageNumber.getCurrentPageNumber(), filmBundle, MainActivity.this);
                } else {
                    loaderManager.restartLoader(pageNumber.getCurrentPageNumber(), filmBundle, MainActivity.this);
                }
            }
        });
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
                    PageNumber pageNumber = (PageNumber) args.getSerializable("pageNumber");
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
                @Nullable
                @Override
                public ArrayList<Film> loadInBackground() {

                    String[] projection = {FilmEntry._ID, FilmEntry.COLUMN_FILM_TITLE, FilmEntry.COLUMN_DATE,
                            FilmEntry.COLUMN_VOTE_AVERAGE, FilmEntry.COLUMN_POSTER, FilmEntry.COLUMN_OVERVIEW};

                    Cursor cursor = getContentResolver().query(CONTENT_URI, projection, null, null,
                            null, null);
                    ArrayList<Film> favFilm = new ArrayList<Film>();


                    try {


                        // Figure out the index of each column
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
            mAdapter.setFilmData(data);

        } else {
            //TODO show error message
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
            mAdapter.setFilmData(null);
            pageNumber = new PageNumber(PageType.POPULARITY, null);
            Bundle filmBundle = new Bundle();
            filmBundle.putSerializable("pageNumber", pageNumber);
            LoaderManager loaderManager = LoaderManager.getInstance(this);
            Loader<ArrayList<Film>> loader = loaderManager.getLoader(pageNumber.getCurrentPageNumber());
            if (loader == null) {
                loaderManager.initLoader(pageNumber.getCurrentPageNumber(), filmBundle, this);
            } else {
                loaderManager.restartLoader(pageNumber.getCurrentPageNumber(), filmBundle, this);
            }
            thisPage.setText("" + pageNumber.getCurrentPageNumber());
            return true;
        } else if (id == R.id.highest_rated && !pageNumber.getCurrentPageSort().equals(NetworkUtils.HIGHEST_RATED)) {

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
            thisPage.setText("" + pageNumber.getCurrentPageNumber());
            return true;
        } else if (id == R.id.favorite && !pageNumber.getCurrentPageSort().equals("FAVORITE")) {
            //TODO get the pages from the database sorted with groups
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
            thisPage.setText("" + pageNumber.getCurrentPageNumber());

        }
        return super.onOptionsItemSelected(item);
    }
}