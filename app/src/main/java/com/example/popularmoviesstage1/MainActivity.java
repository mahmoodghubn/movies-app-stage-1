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

public class MainActivity extends AppCompatActivity implements FilmAdapterOnClickHandler
        , LoaderManager.LoaderCallbacks<ArrayList<Film>> {
    //TODO beautifying decorate the page buttons
    //TODO change the layout of landscape mode for the main view
    //TODO beautifying the detail activity
    //TODO fixing the rotation of the detail activity to start of the part that we left the video before rotation
    //TODO When a user changes the sort criteria (most popular, highest rated, and favorites) the main view gets updated correctly.
    //TODO In the movies detail screen, a user can tap a button (for example, a star) to mark it as a Favorite. Tap the button on a favorite movie will unfavorite it.

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
    private String sort = NetworkUtils.POPULARITY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        thisPage = (TextView) findViewById(R.id.page_number);
        pageNumber = new PageNumber(null,null);

        if (savedInstanceState !=null) {
            pageNumber = (PageNumber) savedInstanceState.getSerializable("pageNumber");
            thisPage.setText(""+pageNumber.getCurrentPageNumber());
        }
        previousPage = findViewById(R.id.ic_left);
        nextPage = findViewById(R.id.ic_right);

        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager gridLayoutManager = new GridLayoutManager(getApplicationContext(), 2);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new FilmAdapter(this);
        Bundle filmBundle = new Bundle();
        filmBundle.putSerializable("pageNumber", pageNumber);
        LoaderManager loaderManager = LoaderManager.getInstance(this);
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
                pageNumber = new PageNumber(null,previousPageNumber);

                filmBundle.putSerializable("pageNumber", pageNumber);
                filmBundle.putString("sortType", sort);
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
                pageNumber = new PageNumber(null,previousPageNumber);
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
        return new AsyncTaskLoader<ArrayList<Film>>(this) {
            @Override
            protected void onStartLoading() {
                super.onStartLoading();
                forceLoad();
            }

            @Nullable
            @Override
            public ArrayList<Film> loadInBackground() {
                PageNumber pageNumber = (PageNumber) args.getSerializable("pageNumber");
                URL filmsRequestUrl = NetworkUtils.createUrl(MainActivity.this,String.valueOf(pageNumber.getCurrentPageNumber()), pageNumber.getCurrentPageSort());

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
        outState.putSerializable("pageNumber",pageNumber);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.most_popular && pageNumber.getCurrentPageSort().equals(NetworkUtils.HIGHEST_RATED)) {
            mAdapter.setFilmData(null);
            pageNumber = new PageNumber(PageType.POPULARITY,null);
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
        } else if (id == R.id.highest_rated && pageNumber.getCurrentPageSort().equals(NetworkUtils.POPULARITY)) {

            mAdapter.setFilmData(null);
            pageNumber = new PageNumber(PageType.HIGH_RATED,null);
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
        }
        return super.onOptionsItemSelected(item);
    }
}
