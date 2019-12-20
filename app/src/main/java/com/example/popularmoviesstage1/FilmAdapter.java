package com.example.popularmoviesstage1;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.view.View.OnClickListener;

import com.example.popularmoviesstage1.Data.FilmContract;
import com.example.popularmoviesstage1.model.Film;
import com.example.popularmoviesstage1.utilities.NetworkUtils;
import com.squareup.picasso.Picasso;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

import static com.example.popularmoviesstage1.DetailActivity.getTarget;
import static com.example.popularmoviesstage1.MainActivity.pageNumber;

public class FilmAdapter extends RecyclerView.Adapter<FilmAdapter.FilmAdapterViewHolder> {

    private Context context;
    private ArrayList<Film> mFilmsData = new ArrayList<>();

    private final FilmAdapterOnClickHandler mClickHandler;

    public interface FilmAdapterOnClickHandler {
        void onClick(Film filmData);
    }

    FilmAdapter(FilmAdapterOnClickHandler clickHandler) {
        mClickHandler = clickHandler;
    }

    public class FilmAdapterViewHolder extends RecyclerView.ViewHolder implements OnClickListener {
        final ImageView mFilmImageView;
        final ImageView favoriteFilmButton;

        FilmAdapterViewHolder(View view) {
            super(view);
            mFilmImageView = view.findViewById(R.id.iv_item);
            favoriteFilmButton = itemView.findViewById(R.id.iv_favButton);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            Film oneFilm = mFilmsData.get(adapterPosition);
            mClickHandler.onClick(oneFilm);
        }
    }

    @NonNull
    @Override
    public FilmAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        context = viewGroup.getContext();
        int layoutIdForListItem = R.layout.film_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;
        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        return new FilmAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final FilmAdapterViewHolder filmAdapterViewHolder, final int position) {
        final Film film = mFilmsData.get(position);
        String poster = film.getPoster();
        String posterURI = film.getPosterURI();

        boolean isInsideDb = false;

        if (posterURI == null) {//we are coming from popular movies or high rated that's why the posterURI is null
            Cursor cursor = new DetailActivity().gainCursorFromDatabase(film.getId(), context);

            try {
                int posterURIColumnIndex = cursor.getColumnIndex(FilmContract.FilmEntry.COLUMN_POSTER_URI);
                //extracting the posterURI from the database if exist
                if (cursor.moveToNext()) {
                    isInsideDb = true;
                    posterURI = cursor.getString(posterURIColumnIndex);
                    film.setPosterURI(posterURI);

                }
            } finally {
                // Always close the cursor when you're done reading from it. This releases all its
                // resources and makes it invalid.
                assert cursor != null;
                cursor.close();
            }
        } else {
            isInsideDb = true;
        }

        if (isInsideDb) {
            filmAdapterViewHolder.favoriteFilmButton.setImageResource(R.drawable.ic_favorite_solid_24dp);
            /*
             * display the image inside the image view
             * if there is no image inside the file download the image from the net*/
            if (!new DetailActivity().loadImageFromStorage(posterURI, filmAdapterViewHolder.mFilmImageView)) {

                //if there was no internet connection when pressing the favorite button the poster will not get saved
                //so we need to reload the poster from the net
                String filmUrl = NetworkUtils.buildPosterUrl(poster, NetworkUtils.ORIGINAL);
                Picasso.with(context)//trying to download and save the poster again
                        .load(filmUrl)
                        .into(getTarget(context, film.getTitle()));
                //after loading and saving the poster getting the poster from the storage and display it
                new DetailActivity().loadImageFromStorage(posterURI, filmAdapterViewHolder.mFilmImageView);
            }

        } else {
            String filmUrl = NetworkUtils.buildPosterUrl(poster, NetworkUtils.w185);
            Picasso.with(context)
                    .load(filmUrl)
                    .into(filmAdapterViewHolder.mFilmImageView);
            filmAdapterViewHolder.favoriteFilmButton.setImageResource(R.drawable.ic_favorite_border_black_24dp);

        }
        final boolean finalIsInsideDb = isInsideDb;
        filmAdapterViewHolder.favoriteFilmButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!finalIsInsideDb) {
                    //we need to set the posterURI with the film we are sending to the datail activity
                    //because if the film is saved the detail activity will load the image from the storage
                    if (film.getPosterURI() == null) {//we came from most popular or high rated movies
                        //that's why the film does not have posterURI
                        ContextWrapper cw = new ContextWrapper(context);
                        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                        // Create imageDir
                        film.setPosterURI(new File(directory, film.getTitle()).getAbsolutePath());
                    }
                    filmAdapterViewHolder.favoriteFilmButton.setImageResource(R.drawable.ic_favorite_solid_24dp);
                    new DetailActivity().insertFilmInDatabase(film, context);
                } else {
                    filmAdapterViewHolder.favoriteFilmButton.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                    new DetailActivity().deleteFilmFromDatabase(film, context);
                }
                if (pageNumber.getCurrentPageSort().equals("FAVORITE")) {
                    mFilmsData.remove(position);
                    notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        if (null == mFilmsData) return 0;
        return mFilmsData.size();
    }

    void setFilmData(ArrayList<Film> filmData) {

        if (filmData == null) {
            mFilmsData.clear();
        } else {
            mFilmsData = filmData;
        }
        notifyDataSetChanged();
    }

}
