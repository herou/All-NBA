package com.gmail.jorgegilcavazos.ballislife.data.service;

import com.gmail.jorgegilcavazos.ballislife.features.model.GameThreadSummary;

import java.util.Map;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RedditGameThreadsService {

    @GET("game_threads/2018-19/.json")
    Single<Map<String, GameThreadSummary>> fetchGameThreads(
            @Query("orderBy") String orderBy,
            @Query("startAt") long startAt,
            @Query("endAt") long endAt);

}
