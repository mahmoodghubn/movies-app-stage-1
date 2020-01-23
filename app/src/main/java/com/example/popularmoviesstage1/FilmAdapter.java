package com.example.popularmoviesstage1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.view.View.OnClickListener;

import com.example.popularmoviesstage1.model.Film;
import com.example.popularmoviesstage1.utilities.NetworkUtils;
import com.squareup.picasso.Picasso;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import static com.example.popularmoviesstage1.MainActivity.pageNumber;

public class FilmAdapter extends RecyclerView.Adapter<FilmAdapter.FilmAdapterViewHolder> {

    private Context context;
    private ArrayList<Film> mFilmsData = new ArrayList<>();

    private final FilmAdapterOnClickHandler mClickHandler;

    public interface FilmAdapterOnClickHandler {
        void onClickItem(Film filmData, View view);
    }

    FilmAdapter(FilmAdapterOnClickHandler clickHandler) {
        mClickHandler = clickHandler;
    }

    public class FilmAdapterViewHolder extends RecyclerView.ViewHolder implements OnClickListener {
        final ImageView mFilmImageView;
        final ImageView favoriteFilmButton ;

        FilmAdapterViewHolder(View view) {
            super(view);
            mFilmImageView =  view.findViewById(R.id.iv_item);
            favoriteFilmButton = itemView.findViewById(R.id.iv_favButton);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            Film oneFilm = mFilmsData.get(adapterPosition);
            mClickHandler.onClickItem(oneFilm, v);
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
    public void onBindViewHolder(@NonNull final FilmAdapterViewHolder filmAdapterViewHolder,final int position) {
        String poster = mFilmsData.get(position).getPoster();
        String filmUrl = NetworkUtils.buildPosterUrl(poster,NetworkUtils.w185);
        Picasso.with(context)
                .load(filmUrl)
                .into(filmAdapterViewHolder.mFilmImageView);

        boolean isInsideDb = new DetailActivity().queryFilmFromDatabase(mFilmsData.get(position).getId(),context);

        if (isInsideDb){
            filmAdapterViewHolder.favoriteFilmButton.setImageResource(R.drawable.ic_favorite_solid_24dp);
        }else {
            filmAdapterViewHolder.favoriteFilmButton.setImageResource(R.drawable.ic_favorite_border_white_24dp);

        }
        filmAdapterViewHolder.favoriteFilmButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isInsideDb = new DetailActivity().queryFilmFromDatabase(mFilmsData.get(position).getId(),context);
                if (!isInsideDb) {
                    filmAdapterViewHolder.favoriteFilmButton.setImageResource(R.drawable.ic_favorite_solid_24dp);
                    new DetailActivity().insertFilmInDatabase(mFilmsData.get(position),context);
                } else {
                    filmAdapterViewHolder.favoriteFilmButton.setImageResource(R.drawable.ic_favorite_border_white_24dp);
                    new DetailActivity().deleteFilmFromDatabase(mFilmsData.get(position).getId(),context);
                }
                if (pageNumber.getCurrentPageSort().equals("FAVORITE")){
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
