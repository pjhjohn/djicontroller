package com.dji.sdk.sample.mrl.network.model;

import java.util.ArrayList;

import dji.common.flightcontroller.DJISimulatorStateData;

public class SimulatorLog {
    public ArrayList<SimulatorEvent> events;
    private boolean isRecording;

    public SimulatorLog() {
        this.events = new ArrayList<>();
        this.isRecording = false;
    }

    public void startRecording() {
        this.events.clear();
        this.isRecording = true;
    }

    public void stopRecording() {
        this.isRecording = false;
    }

    public void add(SimulatorEvent event) {
        if(isRecording) this.events.add(event);
    }

    public void add(DJISimulatorStateData djiSimulatorStateData) {
        if(isRecording) this.events.add(new SimulatorEvent(djiSimulatorStateData));
    }
}
