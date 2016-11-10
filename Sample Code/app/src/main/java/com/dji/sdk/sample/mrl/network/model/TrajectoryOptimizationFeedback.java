package com.dji.sdk.sample.mrl.network.model;

import com.dji.sdk.sample.mrl.VirtualStickCommand;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import dji.thirdparty.rx.Observable;
import dji.thirdparty.rx.android.schedulers.AndroidSchedulers;
import dji.thirdparty.rx.schedulers.Schedulers;

public class TrajectoryOptimizationFeedback {
    public Integer id;                          // Optimization ID
    public Integer current_iteration_index;     // Trajectory Optimization iteration index

    public Integer timestep;                    // VirtualStickCommand interval in milliseconds

    public Boolean success;                     // true if calculation ended without any errors
    public ArrayList<Command> commands;         // Commands to execute. Size 0 for terminate condition

    public String error_message;                // Error message if anything goes wrong (message from server)

    public Observable<VirtualStickCommand> getVirtualStickCommandsObservable() {
        return Observable.from(this.commands)
            .concatMap(command -> Observable.just(command).delay(this.timestep, TimeUnit.MILLISECONDS))
            .map(command -> new VirtualStickCommand(command.pitch, command.roll, command.yaw, command.throttle))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }
}
