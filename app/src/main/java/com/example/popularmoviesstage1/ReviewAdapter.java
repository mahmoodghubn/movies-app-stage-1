package com.example.popularmoviesstage1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View.OnClickListener;

import java.util.ArrayList;



public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewAdapterViewHolder> {

    private Context context;
    private ArrayList<String> mReviewsData = new ArrayList<String>();

    public interface ReviewAdapterOnClickHandler {
        void onClick(String filmData);
    }

    public ReviewAdapter() {
    }

    public class ReviewAdapterViewHolder extends RecyclerView.ViewHolder implements OnClickListener {
        public final TextView mReviewTextView;

        public ReviewAdapterViewHolder(View view) {
            super(view);
            mReviewTextView = (TextView) view.findViewById(R.id.tv_item);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

        }
    }

    @NonNull
    @Override
    public ReviewAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        context = viewGroup.getContext();
        int layoutIdForListItem = R.layout.reveiw_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        return new ReviewAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewAdapterViewHolder reviewAdapterViewHolder, int position) {
        String review = mReviewsData.get(position);
        reviewAdapterViewHolder.mReviewTextView.setText(review);
    }

    @Override
    public int getItemCount() {
        if (null == mReviewsData) return 0;
        return mReviewsData.size();
    }

    public void setReviewData(ArrayList<String> reviewData) {

        if (reviewData == null) {
            mReviewsData.clear();
        } else {
            mReviewsData.addAll(reviewData);
        }
        notifyDataSetChanged();
    }
}

