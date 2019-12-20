package com.example.popularmoviesstage1.Data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.popularmoviesstage1.Data.FilmContract.FilmEntry;

public class FilmDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "films.db";

    private static final int DATABASE_VERSION = 1;

    public FilmDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Create a String that contains the SQL statement to create the pets table
        String SQL_CREATE_FILM_TABLE =  "CREATE TABLE " + FilmEntry.TABLE_NAME + " ("
                + FilmEntry._ID + " INTEGER PRIMARY KEY, "
                + FilmEntry.COLUMN_FILM_TITLE + " TEXT NOT NULL, "
                + FilmEntry.COLUMN_DATE + " TEXT NOT NULL, "
                + FilmEntry.COLUMN_VOTE_AVERAGE + " TEXT NOT NULL, "
                + FilmEntry.COLUMN_OVERVIEW + " TEXT, "
                + FilmEntry.COLUMN_POSTER_URI + " TEXT, "
                + FilmEntry.COLUMN_POSTER + " TEXT );";

        // Execute the SQL statement
        sqLiteDatabase.execSQL(SQL_CREATE_FILM_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
