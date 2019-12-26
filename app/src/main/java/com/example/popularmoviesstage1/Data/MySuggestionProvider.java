package com.example.popularmoviesstage1.Data;

import android.content.SearchRecentSuggestionsProvider;

public class MySuggestionProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "com.example.popularmoviesstage1.Data.MySuggestionProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public MySuggestionProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}