package com.dji.sdk.sample.mrl.network.model;

import com.dji.sdk.sample.mrl.VirtualStickCommand;

import java.util.ArrayList;
import java.util.List;
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
    public Integer timestep;
    public ArrayList<ControlPoint> control_points;
    public ArrayList<State> states;
    public ArrayList<DiffState> diff_states;
    public ArrayList<Command> commands;
    public String created_at;
    public String updated_at;

    public Episode() {
        this.timestep = 200; // For backward compatibility
    }

    public Observable<VirtualStickCommand> getVirtualStickCommandsObservable() {
        ArrayList<Command> commandsToConvert = new ArrayList<>(this.commands);
        commandsToConvert.add(new Command(this.commands.get(this.commands.size() - 1).t + this.timestep, 0.0f, 0.0f, 0.0f, 0.0f));
        return Observable.from(commandsToConvert)
            .concatMap(command -> Observable.just(command).delay(this.timestep, TimeUnit.MILLISECONDS))
            .map(Command::toVirtualStickCommand)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }
}
