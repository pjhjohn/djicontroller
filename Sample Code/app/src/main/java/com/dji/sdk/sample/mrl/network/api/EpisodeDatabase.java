package com.dji.sdk.sample.mrl.network.api;


import com.dji.sdk.sample.mrl.network.model.Command;
import com.dji.sdk.sample.mrl.network.model.Episode;

import java.util.ArrayList;

import dji.thirdparty.retrofit2.http.Field;
import dji.thirdparty.retrofit2.http.GET;
import dji.thirdparty.retrofit2.http.POST;
import dji.thirdparty.rx.Observable;

/**
 * Created by pjhjohn on 2015-10-28.
 */
public interface EpisodeDatabase {
    @POST("episodes")
    Observable<Void> createEpisode(@Field("name") String name, @Field("commands[]") ArrayList<Command> commands);

    @GET("episodes")
    Observable<ArrayList<Episode>> getEpisodes();
}
