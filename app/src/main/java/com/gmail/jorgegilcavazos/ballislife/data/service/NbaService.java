package com.gmail.jorgegilcavazos.ballislife.data.service;

import com.gmail.jorgegilcavazos.ballislife.features.model.BoxScoreResponse;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface NbaService {
    @GET("data/10s/v2015/json/mobile_teams/nba/2018/scores/gamedetail/{gameId}_gamedetail.json")
    Single<BoxScoreResponse> boxScoreNba(@Path("gameId") String gameId);
}
