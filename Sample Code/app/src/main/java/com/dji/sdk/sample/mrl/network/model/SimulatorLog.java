package com.dji.sdk.sample.mrl.network.model;

import java.util.ArrayList;

import dji.common.flightcontroller.DJISimulatorStateData;

public class SimulatorLog {
    public ArrayList<SimulatorEvent> events;
    private boolean isRecording;
    private int timestep;

    public SimulatorLog() {
        this.events = new ArrayList<>();
        this.isRecording = false;
        this.timestep = 0;
    }

    public void startRecording(int timestep) {
        this.events.clear();
        this.isRecording = true;
        this.timestep = timestep;
    }

    public void stopRecording() {
        this.isRecording = false;
        this.timestep = 0;
    }

    public void add(DJISimulatorStateData djiSimulatorStateData) {
        if(isRecording) this.events.add(new SimulatorEvent(djiSimulatorStateData, this.timestep * this.events.size()));
    }
}
