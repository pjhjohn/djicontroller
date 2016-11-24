package com.dji.sdk.sample.mrl.network.model;

import dji.common.flightcontroller.DJISimulatorStateData;

public class SimulatorEvent {
    public Double t;
    public Float x;
    public Float y;
    public Float z;
    public Float rx;
    public Float ry;
    public Float rz;

    public SimulatorEvent(DJISimulatorStateData simulatorStateData, long elapsedTime) {
        this.rx = simulatorStateData.getRoll();
        this.ry = -simulatorStateData.getPitch();
        this.rz = -simulatorStateData.getYaw();
        this.t = (double) elapsedTime;
        this.x = simulatorStateData.getPositionX();
        this.y = -simulatorStateData.getPositionY();
        this.z = -simulatorStateData.getPositionZ();
    }
}
