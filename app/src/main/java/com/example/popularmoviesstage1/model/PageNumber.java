package com.example.popularmoviesstage1.model;

import androidx.annotation.Nullable;

import com.example.popularmoviesstage1.utilities.NetworkUtils;

import java.io.Serializable;

public class PageNumber implements Serializable {
    public enum PageType {
        POPULARITY,
        HIGH_RATED
    }

    private static PageType pageSort = PageType.POPULARITY;
    private static Integer high_rated_page_number = 1;
    private static Integer popularity_page_number = 1;

    public PageNumber(@Nullable PageType pageType, @Nullable Integer pageNumber) {
        if (pageType != null) {
            pageSort = pageType;
        }
        if (pageNumber != null) {
            if (pageSort == PageType.POPULARITY) {
                popularity_page_number = pageNumber;
            } else if (pageSort == PageType.HIGH_RATED) {
                high_rated_page_number = pageNumber;
            }
        }
    }


    public  String getCurrentPageSort() {
        if (pageSort == PageType.POPULARITY) {
            return NetworkUtils.POPULARITY;
        } else {
            return NetworkUtils.HIGHEST_RATED;
        }
    }

    public  int getCurrentPageNumber() {
        if (pageSort == PageType.POPULARITY) {
            return popularity_page_number;
        } else {
            return high_rated_page_number;
        }
    }
}
