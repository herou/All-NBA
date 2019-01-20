package com.gmail.jorgegilcavazos.ballislife.data.service;

import com.gmail.jorgegilcavazos.ballislife.features.model.BoxScoreResponse;
import com.gmail.jorgegilcavazos.ballislife.features.model.GameV2;

import java.util.Map;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NbaGamesService {

    @GET("games/2018-19/.json")
    Single<Map<String, GameV2>> getDayGames(
            @Query("orderBy") String orderBy,
            @Query("startAt") long startAt, @Query("endAt") long endAt);

    @GET("boxscore/{gameId}/.json")
    Single<BoxScoreResponse> boxScore(@Path("gameId") String gameId);

    @GET("games/2018-19/{gameId}/.json")
    Single<GameV2> getGame(@Path("gameId") String gameId);
}
