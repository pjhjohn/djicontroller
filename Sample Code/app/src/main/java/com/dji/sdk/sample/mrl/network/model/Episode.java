package com.dji.sdk.sample.mrl.network.model;

import com.dji.sdk.sample.mrl.VirtualStickCommand;

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
    public List<ControlPoint> control_points;
    public List<State> states;
    public List<DiffState> diff_states;
    public List<Command> commands;
    public String created_at;
    public String updated_at;

    public Episode() {
        this.timestep = 200; // For backward compatibility
    }

    public Observable<VirtualStickCommand> getVirtualStickCommandsObservable() {
        return Observable.from(this.commands)
            .concatMap(command -> Observable.just(command).delay(this.timestep, TimeUnit.MILLISECONDS))
            .map(command -> new VirtualStickCommand(command.pitch, command.roll, command.yaw, command.throttle))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }
}
