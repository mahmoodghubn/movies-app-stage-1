package com.example.popularmoviesstage1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.example.popularmoviesstage1.model.Film;
import com.example.popularmoviesstage1.utilities.NetworkUtils;

import java.net.URL;
import java.util.List;
import com.example.popularmoviesstage1.FilmAdapter.FilmAdapterOnClickHandler;

public class MainActivity extends AppCompatActivity implements FilmAdapterOnClickHandler {

    private FilmAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.rv);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getApplicationContext(),2);
//        LinearLayoutManager gridLayoutManager
//                = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new FilmAdapter(this);

        new FetchFilmData().execute(NetworkUtils.MOST_POPULAR_MOVIES_API);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onClick(String weatherForDay) {

    }

    @SuppressLint("StaticFieldLeak")
    private class FetchFilmData extends AsyncTask<String, Void, String[]>{

        @Override
        protected String[] doInBackground(String... params) {
            /* If there's no zip code, there's nothing to look up. */
            if (params.length == 0) {
                return null;
            }

            String filmsUrl = params[0];
            URL filmsRequestUrl = NetworkUtils.createUrl(filmsUrl);

            try {
                String jsonFilmsResponse = NetworkUtils
                        .getResponseFromHttpUrl(filmsRequestUrl);

                List<Film> simpleJsonFilmsData = NetworkUtils
                        .extractFeatureFromJson(MainActivity.this, jsonFilmsResponse);
                String[] films = new String[simpleJsonFilmsData.size()];

                for (int i = 0 ;i<simpleJsonFilmsData.size();i++) {
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
            if (strings != null){
                mAdapter.setFilmData(strings);
            }else {
                //TODO show error message
            }
        }
    }
}
