package com.example.popularmoviesstage1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.example.popularmoviesstage1.model.Film;
import com.example.popularmoviesstage1.utilities.NetworkUtils;

import java.net.URL;
import java.util.List;

import com.example.popularmoviesstage1.FilmAdapter.FilmAdapterOnClickHandler;

public class MainActivity extends AppCompatActivity implements FilmAdapterOnClickHandler {

    private FilmAdapter mAdapter;
    private static int pageNumber = 1;
    static boolean isPopularityUsed =true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.rv);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getApplicationContext(), 2);
//        LinearLayoutManager gridLayoutManager
//                = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new FilmAdapter(this);

        new FetchFilmData().execute("1", NetworkUtils.POPULARITY);
        //TODO take the value of sort_by from menu
//        new FetchFilmData().execute(NetworkUtils.MOST_POPULAR_MOVIES_API);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)) {
                    //TODO take sort by from another place
                    pageNumber++;
                    new FetchFilmData().execute(Integer.toString(pageNumber), NetworkUtils.POPULARITY);

                }
            }
        });
    }

    @Override
    public void onClick(String weatherForDay) {

    }

    @SuppressLint("StaticFieldLeak")
    private class FetchFilmData extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... params) {
            /* If there's no zip code, there's nothing to look up. */
            if (params.length == 0) {
                return null;
            }

            String pageNumber = params[0];
            String sort_by = params[1];
            URL filmsRequestUrl = NetworkUtils.createUrl(pageNumber, sort_by);

            try {
                String jsonFilmsResponse = NetworkUtils
                        .getResponseFromHttpUrl(filmsRequestUrl);

                List<Film> simpleJsonFilmsData = NetworkUtils
                        .extractFeatureFromJson(MainActivity.this, jsonFilmsResponse);
                String[] films = new String[simpleJsonFilmsData.size()];
                for (int i = 0; i < simpleJsonFilmsData.size(); i++) {
                    films[i] = simpleJsonFilmsData.get(i).getPoster();
                }

                return films;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String[] strings) {
            if (strings != null) {
                mAdapter.setFilmData(strings);
            } else {
                //TODO show error message
            }
        }
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.most_popular && !isPopularityUsed) {
            mAdapter.setFilmData(null);
            isPopularityUsed = !isPopularityUsed;
            new FetchFilmData().execute("1", NetworkUtils.POPULARITY);
            return true;
        } else if (id == R.id.highest_rated && isPopularityUsed) {
            mAdapter.setFilmData(null);
            isPopularityUsed = !isPopularityUsed;
            new FetchFilmData().execute("1", NetworkUtils.HIGHEST_RATED);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
