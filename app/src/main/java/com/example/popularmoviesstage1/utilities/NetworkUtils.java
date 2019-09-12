package com.example.popularmoviesstage1.utilities;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.example.popularmoviesstage1.model.Film;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

public class NetworkUtils {
    private final static String API_KEY = "api_key";
    private final static String API_KEY_NUMBER = "1bdd3d05d7dbf4ce67ee2abb8f9bfe78";
    public final static String POPULARITY = "popular";
    public final static String HIGHEST_RATED = "top_rated";


    private final static String POSTER_PATH = "https://image.tmdb.org/t/p/";
    public final static String ORIGINAL = "original";
    public final static String w185 = "w185";
    private static final String LOG_TAG = NetworkUtils.class.getSimpleName();
    private final static String MOVIES_BASE_URL = "http://api.themoviedb.org/3/movie/";
    private final static String PAGE_NUMBER = "page";

    private final static String GETTING_MOVIES_KEY_URL = "http://api.themoviedb.org/3/movie/%1$s";


    /**
     * Returns new URL object from the given string URL.
     */
    public static URL createUrl(String page_number, String sort_by) {

        Uri builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                .appendPath(sort_by)
                .appendQueryParameter(API_KEY, API_KEY_NUMBER)
                .appendQueryParameter(PAGE_NUMBER, page_number)
                .build();

        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Problem building the URL ", e);
        }
        return url;
    }

    public static URL creatingKeyUrl(String id,String type) {
        String keyBaseUrl = String.format(GETTING_MOVIES_KEY_URL, id);
        Uri buildKeyUri = Uri.parse(keyBaseUrl).buildUpon()
                .appendPath(type)
                .appendQueryParameter(API_KEY, API_KEY_NUMBER)
                .build();
        URL url = null;
        try {
            url = new URL(buildKeyUri.toString());
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Problem building the URL ", e);
        }
        return url;
    }


    public static ArrayList<String> extractKeysFromJson(Context context, String keysJSON,String query) {
        if (TextUtils.isEmpty(keysJSON)) {
            return null;
        }
        ArrayList<String> keys = new ArrayList<>();
        try {

            // Create a JSONObject from the JSON response string
            JSONObject baseJsonResponse = new JSONObject(keysJSON);

            // Extract the JSONArray associated with the key called "features",
            // which represents a list of features (or films).
            JSONArray keysArray;
            keysArray = baseJsonResponse.getJSONArray("results");



            // For each films in the filmsArray, create an {@link films} object
            for (int i = 0; i < keysArray.length(); i++) {

                // Get a single key at position i within the list of keys
                JSONObject currentFilm = keysArray.getJSONObject(i);
                String key;
                if (query.equals("videos")) {
                    key = currentFilm.getString("key");
                }else {
                    key = currentFilm.getString("content");
                }

                keys.add(key);

            }
        } catch (JSONException e) {

            Log.e("QueryUtils", "Problem parsing the films JSON results", e);
        }
        // Return the list of films
        return keys;


    }


    public static String getResponseFromHttpUrl(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();
            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");
            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                return scanner.next();
            } else {
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    public static ArrayList<Film> extractFeatureFromJson(Context context, String filmsJSON) {
        // If the JSON string is empty or null, then return early.
        if (TextUtils.isEmpty(filmsJSON)) {
            return null;
        }

        // Create an empty ArrayList that we can start adding films to
        ArrayList<Film> films = new ArrayList<>();

        // Try to parse the JSON response string. If there's a problem with the way the JSON
        // is formatted, a JSONException exception object will be thrown.
        // Catch the exception so the app doesn't crash, and print the error message to the logs.
        try {

            // Create a JSONObject from the JSON response string
            JSONObject baseJsonResponse = new JSONObject(filmsJSON);

            // Extract the JSONArray associated with the key called "features",
            // which represents a list of features (or films).
            JSONArray filmsArray = baseJsonResponse.getJSONArray("results");

            // For each films in the filmsArray, create an {@link films} object
            for (int i = 0; i < filmsArray.length(); i++) {

                // Get a single film at position i within the list of films
                JSONObject currentFilm = filmsArray.getJSONObject(i);
                String posterPath = currentFilm.getString("poster_path");
                String title = currentFilm.getString("title");
                String overview = currentFilm.getString("overview");
                String releaseDate = currentFilm.getString("release_date");
                String voteAverage = currentFilm.getString("vote_average");
                String id = currentFilm.getString("id");
                if (!posterPath.equals("null")) {
                    //TODO canceling this section and put a dummy photo when there is no photo
                    Film film = new Film(posterPath, title, overview, releaseDate, voteAverage, id);
                    films.add(film);
                }
            }
        } catch (JSONException e) {

            Log.e("QueryUtils", "Problem parsing the films JSON results", e);
        }
        // Return the list of films
        return films;
    }
    public static String buildPosterUrl(String poster,String width) {

        return POSTER_PATH + width + "/" + poster;

    }
}
