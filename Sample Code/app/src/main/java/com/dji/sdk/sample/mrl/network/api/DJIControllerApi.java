package com.dji.sdk.sample.mrl.network.api;


import com.dji.sdk.sample.mrl.network.model.Episode;
import com.dji.sdk.sample.mrl.network.model.SimulatorLog;

import java.util.ArrayList;

import dji.thirdparty.retrofit2.Call;
import dji.thirdparty.retrofit2.http.Body;
import dji.thirdparty.retrofit2.http.GET;
import dji.thirdparty.retrofit2.http.POST;
import dji.thirdparty.retrofit2.http.Path;

/**
 * Created by pjhjohn on 2015-10-28.
 */
public interface DJIControllerApi {
    @GET("episodes.json")
    Call<ArrayList<Episode>> readEpisodes();

    @GET("episodes/{id}.json")
    Call<ArrayList<Episode>> readEpisode(@Path("id") Integer id);

    @POST("episodes/{id}/update_simulator_log.json")
    Call<Void> pushSimulatorLog(@Path("id") Integer id, @Body SimulatorLog simulatorLog);
}
