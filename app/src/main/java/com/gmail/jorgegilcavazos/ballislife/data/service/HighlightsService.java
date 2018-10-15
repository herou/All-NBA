package com.gmail.jorgegilcavazos.ballislife.data.service;

import com.gmail.jorgegilcavazos.ballislife.features.model.Highlight;

import java.util.Map;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface HighlightsService {

    @GET("v2/highlights/2018-19/all.json")
    Single<Map<String, Highlight>> getAllHighlights(@Query("orderBy") String orderBy,
                                                    @Query("startAt") long startAt,
                                                    @Query("endAt") long endAt,
                                                    @Query("limitToLast") int limitToLast);

    @GET("v2/highlights/2018-19/{date}.json")
    Single<Map<String, Highlight>> getDailyHighlights(@Path("date") String date,
                                                      @Query("orderBy") String orderBy,
                                                      @Query("startAt") int startAt,
                                                      @Query("endAt") int endAt,
                                                      @Query("limitToLast") int limitToLast);

    @GET("v2/highlights/2018-19/{week}.json")
    Single<Map<String, Highlight>> getWeeklyHighlights(@Path("week") String week,
                                                       @Query("orderBy") String orderBy,
                                                       @Query("startAt") int startAt,
                                                       @Query("endAt") int endAt,
                                                       @Query("limitToLast") int limitToLast);

    @GET("v2/highlights/2018-19/all.json")
    Single<Map<String, Highlight>> getSeasonHighlights(@Query("orderBy") String orderBy,
                                                       @Query("startAt") int startAt,
                                                       @Query("endAt") int endAt,
                                                       @Query("limitToLast") int limitToLast);
}
