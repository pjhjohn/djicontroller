package com.dji.sdk.sample.mrl.network.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
    private Api(DJIControllerApi djiControllerApi) {
        this.DJIController = djiControllerApi;
    }

    /* Only triggered at first */
    public static synchronized void createInstance() {
        if(Api.instance != null) return;

        Gson gson = new GsonBuilder().setLenient().create();

        Api.instance = new Api(new Retrofit.Builder()
            .baseUrl("https://djicontroller-server-pjhjohn.c9users.io/")
            .addConverterFactory(GsonConverterFactory.create(gson))
//            .addCallAdapterFactory(RxJavaCallAdapterFactory.create()) //Rxandroid를 사용하기 위해 추가(옵션)
            .build()
            .create(DJIControllerApi.class)
        );
    }

    /* Register Papyruth Api*/
    private final DJIControllerApi DJIController;
    public static DJIControllerApi controller() {
        return Api.instance.DJIController;
    }
}
