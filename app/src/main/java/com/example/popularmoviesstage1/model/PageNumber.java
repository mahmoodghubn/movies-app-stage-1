package com.example.popularmoviesstage1.model;

import androidx.annotation.Nullable;

import com.example.popularmoviesstage1.utilities.NetworkUtils;

import java.io.Serializable;

public class PageNumber implements Serializable {
    public enum PageType {
        POPULARITY,
        TOP_RATED,
        FAVORITE,
        SEARCH
    }

    private static PageType pageSort = PageType.POPULARITY;
    private static Integer top_rated_page_number = 1;
    private static Integer popularity_page_number = 1;
    private static Integer favorite_page_number = 1;
    private static Integer search_page_number = 1;

    public static Integer getHigh_rated_page_number() {
        return top_rated_page_number;
    }

    public static Integer getPopularity_page_number() {
        return popularity_page_number;
    }

    public static Integer getFavorite_page_number() {
        return favorite_page_number;
    }

    public static Integer getSearch_page_number() {
        return search_page_number;
    }

    public static void setPageSort(String pageSort) {
        switch (pageSort) {
            case "POPULARITY":
                PageNumber.pageSort = PageType.POPULARITY;
                break;
            case "SEARCH":
                PageNumber.pageSort = PageType.SEARCH;
                break;
            case "FAVORITE":
                PageNumber.pageSort = PageType.FAVORITE;
                break;
            case "TOP_RATED":
                PageNumber.pageSort = PageType.TOP_RATED;
                break;
        }
    }

    public static void setHigh_rated_page_number(Integer top_rated_page_number) {
        PageNumber.top_rated_page_number = top_rated_page_number;
    }

    public static void setPopularity_page_number(Integer popularity_page_number) {
        PageNumber.popularity_page_number = popularity_page_number;
    }

    public static void setFavorite_page_number(Integer favorite_page_number) {
        PageNumber.favorite_page_number = favorite_page_number;
    }

    public static void setSearch_page_number(Integer search_page_number) {
        PageNumber.search_page_number = search_page_number;
    }

    public PageNumber(@Nullable PageType pageType, @Nullable Integer pageNumber) {
        if (pageType != null) {
            pageSort = pageType;
        }
        if (pageNumber != null) {
            switch (pageSort) {
                case POPULARITY:
                    setPopularity_page_number(pageNumber);
                    break;
                case SEARCH:
                    setSearch_page_number(pageNumber);
                    break;
                case FAVORITE:
                    setFavorite_page_number(pageNumber);
                    break;
                case TOP_RATED:
                    setHigh_rated_page_number(pageNumber);
                    break;
            }
        }
    }

    public String getCurrentPageSort() {
        switch (pageSort) {
            case POPULARITY:
                return NetworkUtils.POPULARITY;
            case TOP_RATED:
                return NetworkUtils.HIGHEST_RATED;
            case SEARCH:
                return "SEARCH";
            case FAVORITE:
                return "FAVORITE";
        }
        return "";
    }

    public int getCurrentPageNum() {
        switch (pageSort) {
            case POPULARITY:
                return popularity_page_number;
            case TOP_RATED:
                return top_rated_page_number;
            case SEARCH:
                return search_page_number;
            case FAVORITE:
                return favorite_page_number;
        }
        return 1;
    }
}