package com.example.popularmoviesstage1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.view.View.OnClickListener;

import com.example.popularmoviesstage1.model.Film;
import com.squareup.picasso.Picasso;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class FilmAdapter extends RecyclerView.Adapter<FilmAdapter.FilmAdapterViewHolder> {

    private Context context;
    private ArrayList<Film> mFilmsData = new ArrayList<Film>();

    private final FilmAdapterOnClickHandler mClickHandler;

    public interface FilmAdapterOnClickHandler {
        void onClick(Film filmData);
    }

    public FilmAdapter(FilmAdapterOnClickHandler clickHandler) {
        mClickHandler = clickHandler;
    }

    public class FilmAdapterViewHolder extends RecyclerView.ViewHolder implements OnClickListener {
        public final ImageView mFilmImageView;

        public FilmAdapterViewHolder(View view) {
            super(view);
            mFilmImageView = (ImageView) view.findViewById(R.id.iv_item);
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
    public void onBindViewHolder(@NonNull FilmAdapterViewHolder filmAdapterViewHolder, int position) {
        String filmUrl = mFilmsData.get(position).getPoster();
        Picasso.with(context)
                .load(filmUrl)
                .into(filmAdapterViewHolder.mFilmImageView);
    }

    @Override
    public int getItemCount() {
        if (null == mFilmsData) return 0;
        return mFilmsData.size();
    }

    public void setFilmData(ArrayList<Film> filmData) {

        if (filmData == null) {
            mFilmsData.clear();
        } else {
            mFilmsData.addAll(filmData);
        }
        notifyDataSetChanged();
    }
}