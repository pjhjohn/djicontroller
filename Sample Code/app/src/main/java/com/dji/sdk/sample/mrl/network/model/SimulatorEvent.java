package com.dji.sdk.sample.mrl.network.model;

import dji.common.flightcontroller.DJISimulatorStateData;

public class SimulatorEvent {
    public Double latitude;
    public Double longitude;
    public Float yaw;
    public Float pitch;
    public Float roll;
    public Float positionX;
    public Float positionY;
    public Float positionZ;
    public Boolean isFlying;
    public Boolean areMotorsOn;
    public Long time;

    public SimulatorEvent(DJISimulatorStateData simulatorStateData, long startTime) {
        this.latitude = simulatorStateData.getLatitude();
        this.longitude = simulatorStateData.getLongitude();
        this.yaw = simulatorStateData.getYaw();
        this.pitch = simulatorStateData.getPitch();
        this.roll = simulatorStateData.getRoll();
        this.positionX = simulatorStateData.getPositionX();
        this.positionY = simulatorStateData.getPositionY();
        this.positionZ = simulatorStateData.getPositionZ();
        this.isFlying = simulatorStateData.isFlying();
        this.areMotorsOn = simulatorStateData.areMotorsOn();
        this.time = System.currentTimeMillis() - startTime;
    }
}
