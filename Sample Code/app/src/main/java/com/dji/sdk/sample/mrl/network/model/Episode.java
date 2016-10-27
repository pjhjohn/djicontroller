package com.dji.sdk.sample.mrl.network.model;

import com.dji.sdk.sample.mrl.VirtualStickCommand;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import dji.thirdparty.rx.Observable;
import dji.thirdparty.rx.android.schedulers.AndroidSchedulers;
import dji.thirdparty.rx.schedulers.Schedulers;

/**
 * Created by User on 2016-07-31.
 */

public class Episode {
    public Integer id;
    public String name;
    public ArrayList<Command> commands;
    public ArrayList<VirtualStickCommand> virtualStickCommands;

    public Episode() {
        this("");
    }
    public Episode(String name) {
        this.name = name;
        this.virtualStickCommands = new ArrayList<>();
        this.commands = new ArrayList<>();
    }

    public Episode push(VirtualStickCommand virtualStickCommand) {
        int index = this.virtualStickCommands.size();
        this.virtualStickCommands.add(virtualStickCommand.setIndex(index));
        this.commands.add(virtualStickCommand.setIndex(index).toEpisodeCommand());
        return this;
    }

    public Observable<VirtualStickCommand> getEpisodeObservable() {
        return Observable.from(this.virtualStickCommands)
            .concatMap(command -> Observable.just(command).delay(200, TimeUnit.MILLISECONDS))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }
}
