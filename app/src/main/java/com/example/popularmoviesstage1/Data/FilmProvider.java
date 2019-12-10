package com.example.popularmoviesstage1.Data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FilmProvider extends ContentProvider {

    public static final String LOG_TAG = FilmProvider.class.getSimpleName();

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int Film = 1;
    private static final int Film_ID = 2;
    static {

        sUriMatcher.addURI(FilmContract.CONTENT_AUTHORITY,FilmContract.PATH_FILM,Film);
        sUriMatcher.addURI(FilmContract.CONTENT_AUTHORITY,FilmContract.PATH_FILM +"/#",Film_ID);
    }

    private FilmDbHelper filmDbHelper;
    @Override
    public boolean onCreate() {
        filmDbHelper = new FilmDbHelper(getContext());

        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase database = filmDbHelper.getReadableDatabase();
        Cursor cursor;
        int match = sUriMatcher.match(uri);
        switch (match){
            case Film:
                cursor = database.query(FilmContract.FilmEntry.TABLE_NAME,projection,selection,selectionArgs
                        ,null,null,null);
                break;
            case Film_ID:
                selection = FilmContract.FilmEntry._ID+"=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(FilmContract.FilmEntry.TABLE_NAME,projection,selection,selectionArgs
                        ,null,null,sortOrder);

                break;
            default:
                throw new IllegalArgumentException("unknown uri");
        }
        //cursor.setNotificationUri(getContext().getContentResolver(),uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        final int match = sUriMatcher.match(uri);
        SQLiteDatabase database = filmDbHelper.getReadableDatabase();
        long id ;
        if (match == Film) {
            id = database.insert(FilmContract.FilmEntry.TABLE_NAME, null, values);
            if (id == -1) {
                Log.e(LOG_TAG, "Failed to insert row for " + uri);
                return null;
            }
            //getContext().getContentResolver().notifyChange(uri,null);

            // Return the new URI with the ID (of the newly inserted row) appended at the end
            return ContentUris.withAppendedId(uri, id);
        }
        throw new IllegalArgumentException("insertion is not supported for " + uri);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        SQLiteDatabase database = filmDbHelper.getReadableDatabase();
        int id ;
        if (match == Film) {
            id = database.delete(FilmContract.FilmEntry.TABLE_NAME, selection, selectionArgs);
            if (id == -1) {
                Log.e(LOG_TAG, "Failed to delete row for " + uri);
            }
            //getContext().getContentResolver().notifyChange(uri,null);

            // Return the new URI with the ID (of the newly inserted row) appended at the end
            return id;
        }
        throw new IllegalArgumentException("delete is not supported for " + uri);
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
