package com.example.popularmoviesstage1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.example.popularmoviesstage1.model.Film;
import com.example.popularmoviesstage1.utilities.NetworkUtils;

import java.net.URL;
import java.util.List;
import com.example.popularmoviesstage1.FilmAdapter.FilmAdapterOnClickHandler;

import static android.widget.LinearLayout.VERTICAL;

public class MainActivity extends AppCompatActivity implements FilmAdapterOnClickHandler {

    private FilmAdapter mAdapter;
    private RecyclerView mRecyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = (RecyclerView)findViewById(R.id.rv);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this,2 );
//        LinearLayoutManager gridLayoutManager
//                = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new FilmAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        new FetchFilmData().execute(NetworkUtils.MOST_POPULAR_MOVIES_API);



    }

    @Override
    public void onClick(String weatherForDay) {

    }

    public class FetchFilmData extends AsyncTask<String, Void, String[]>{

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
                Log.i("doInBackground","getResponseFromHttpUrl");
                Log.i("doInBackground",jsonFilmsResponse);



                List<Film> simpleJsonFilmsData = NetworkUtils
                        .extractFeatureFromJson(MainActivity.this, jsonFilmsResponse);
                String[] films = new String[simpleJsonFilmsData.size()];
                Log.i("doInBackground","simpleJsonFilmsData");

                for (int i = 0 ;i<simpleJsonFilmsData.size();i++) {
                    films[i] = simpleJsonFilmsData.get(i).getPoster();
                    Log.i("doInBackground",films[i]);

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
                Log.i("onPostExecute","after set film data");
            }else {
                //TODO show error message
            }
        }

    }
}
