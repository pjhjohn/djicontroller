package com.dji.sdk.sample.mrl.network.api;


import com.dji.sdk.sample.mrl.network.model.Episode;

import java.util.ArrayList;

import dji.thirdparty.retrofit2.Call;
import dji.thirdparty.retrofit2.http.Body;
import dji.thirdparty.retrofit2.http.GET;
import dji.thirdparty.retrofit2.http.POST;
import dji.thirdparty.retrofit2.http.Path;

/**
 * Created by pjhjohn on 2015-10-28.
 */
public interface EpisodeDatabase {
    @GET("episodes.json")
    Call<ArrayList<Episode>> getEpisodes();

    @GET("episodes/{id}.json")
    Call<ArrayList<Episode>> getEpisode(@Path("id") Integer id);
}
