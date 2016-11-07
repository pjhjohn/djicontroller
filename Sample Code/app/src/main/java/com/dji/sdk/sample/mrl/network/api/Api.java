package com.dji.sdk.sample.mrl.network.api;

import dji.thirdparty.retrofit2.Retrofit;
import dji.thirdparty.retrofit2.converter.gson.GsonConverterFactory;


/**
 * Created by pjhjohn on 2015-10-31.
 */
public class Api {
    /* Singleton */
    private static Api instance = null;
    public static Api getInstance() {
        return instance;
    }

    /* Instance Creation */
    private Api(EpisodeDatabase EpisodeDatabaseApi) {
        this.EpisodeDatabaseApi = EpisodeDatabaseApi;
    }

    /* Only triggered at first */
    public static synchronized void createInstance() {
        if(Api.instance != null) return;
        Api.instance = new Api(new Retrofit.Builder()
            .baseUrl("https://djicontroller-server-pjhjohn.c9users.io/")
            .addConverterFactory(GsonConverterFactory.create())
//            .addCallAdapterFactory(RxJavaCallAdapterFactory.create()) //Rxandroid를 사용하기 위해 추가(옵션)
            .build()
            .create(EpisodeDatabase.class)
        );
    }

    /* Register Papyruth Api*/
    private final EpisodeDatabase EpisodeDatabaseApi;
    public static EpisodeDatabase database() {
        return Api.instance.EpisodeDatabaseApi;
    }
}
