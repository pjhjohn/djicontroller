package com.dji.sdk.sample.mrl.network.model;

import java.util.ArrayList;

import dji.common.flightcontroller.DJISimulatorStateData;
import dji.thirdparty.rx.Observable;
import dji.thirdparty.rx.subjects.BehaviorSubject;

public class SimulatorLog {
    private static SimulatorLog instance;
    private SimulatorLog() {}
    public static synchronized SimulatorLog getInstance() {
        if (SimulatorLog.instance == null) SimulatorLog.instance = new SimulatorLog();
        return SimulatorLog.instance;
    }

    private BehaviorSubject<Integer> mRecordingSubject;
    public ArrayList<SimulatorEvent> events = new ArrayList<>(); // Will be passed to the server with parameter name equal to member variable name
    private boolean isRecording = false;
    private int mTimestep = 0;
    private int mRecordIndex = 0;
    private int mRecordCount = 0;
    private final int mRecordOffset = 2; // Number of logs to ignore at the beginning

    public Observable<Integer> startRecording(int timestep, int recordCount) {
        isRecording = true;
        events.clear();
        mTimestep = timestep;
        mRecordIndex = 0;
        mRecordCount = recordCount;
        return mRecordingSubject = BehaviorSubject.create();
    }

    private void stopRecording() {
        isRecording = false;
        mTimestep = 0;
        mRecordingSubject.onNext(events.size());
        mRecordingSubject.onCompleted();
    }

    public void add(DJISimulatorStateData djiSimulatorStateData) {
        if(isRecording) {
            if (mRecordIndex < mRecordOffset + mRecordCount) {
                if(mRecordIndex >= mRecordOffset) events.add(new SimulatorEvent(djiSimulatorStateData, mTimestep * events.size()));
                mRecordIndex++;
            } else stopRecording();
        }
    }
}
