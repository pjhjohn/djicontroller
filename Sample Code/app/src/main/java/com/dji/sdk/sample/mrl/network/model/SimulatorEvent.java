package com.dji.sdk.sample.mrl.network.model;

import dji.common.flightcontroller.DJISimulatorStateData;

public class SimulatorEvent {
    public Float rx;
    public Float ry;
    public Float rz;
    public Long t;
    public Float x;
    public Float y;
    public Float z;

    public SimulatorEvent(DJISimulatorStateData simulatorStateData, long elapsedTime) {
        this.rx = simulatorStateData.getRoll();
        this.ry = -simulatorStateData.getPitch();
        this.rz = -simulatorStateData.getYaw();
        this.t = elapsedTime;
        this.x = simulatorStateData.getPositionX();
        this.y = simulatorStateData.getPositionY();
        this.z = simulatorStateData.getPositionZ();
    }
}
