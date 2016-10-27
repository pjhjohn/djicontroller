package com.dji.sdk.sample.mrl;

import com.dji.sdk.sample.mrl.network.model.Command;

import dji.common.flightcontroller.DJIFlightControllerDataType;
import dji.common.flightcontroller.DJIVirtualStickFlightControlData;

/**
 * Created by pjhjohn on 10/17/16.
 */

public class VirtualStickCommand {
    public static float MAX_SPEED_PITCH = DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMaxVelocity;
    public static float MAX_SPEED_ROLL = DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMaxVelocity;
    public static float MAX_SPEED_THROTTLE = DJIFlightControllerDataType.DJIVirtualStickVerticalControlMaxVelocity;
    public static float MAX_SPEED_YAW = DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity;

    private int index;
    private float pitch, roll, yaw, throttle;

    public enum Direction {
        FORWARD, BACKWARD, UP, DOWN, LEFT, RIGHT, CW, CCW
    }

    public VirtualStickCommand(Direction direction) {
        float pitch = 0, roll = 0, yaw = 0, throttle = 0;
        switch (direction) {
            case FORWARD  : pitch    = -0.5f; break;
            case BACKWARD : pitch    =  0.5f; break;
            case LEFT     : roll     = -0.5f; break;
            case RIGHT    : roll     =  0.5f; break;
            case CCW      : yaw      = -0.5f; break;
            case CW       : yaw      =  0.5f; break;
            case UP       : throttle =  1.0f; break;
            case DOWN     : throttle =  0.0f; break;
        } this.set(0, pitch, roll, yaw, throttle);
    }
    public VirtualStickCommand() {
        this.set(0, 0, 0, 0, 0);
    }
    public VirtualStickCommand(int index) {
        this.set(index, 0, 0, 0, 0);
    }
    public VirtualStickCommand(int index, float pitch, float roll, float yaw, float throttle) {
        this.set(index, pitch, roll, yaw, throttle);
    }

    private void set(int index, float pitch, float roll, float yaw, float throttle) {
        this.setIndex(index);
        this.setPitch(pitch);
        this.setRoll(roll);
        this.setYaw(yaw);
        this.setThrottle(throttle);
    }

    public int getIndex() {
        return index;
    }

    public VirtualStickCommand setIndex(int index) {
        this.index = index;
        return this;
    }

    public float getPitch() {
        return pitch * MAX_SPEED_PITCH;
    }

    public VirtualStickCommand setPitch(float pitch) {
        this.pitch = clipped(pitch);
        return this;
    }

    public float getRoll() {
        return roll * MAX_SPEED_ROLL;
    }

    public VirtualStickCommand setRoll(float roll) {
        this.roll = clipped(roll);
        return this;
    }

    public float getYaw() {
        return yaw * MAX_SPEED_YAW;
    }

    public VirtualStickCommand setYaw(float yaw) {
        this.yaw = clipped(yaw);
        return this;
    }

    public float getThrottle() {
        return throttle * MAX_SPEED_THROTTLE;
    }

    public VirtualStickCommand setThrottle(float throttle) {
        this.throttle = clipped(throttle);
        return this;
    }
    private float clipped(float value) {
        if (value > 1.0f) return 1.0f;
        else if (value < -1.0f) return -1.0f;
        else return value;
    }

    @Override
    public String toString() {
        return String.format("CMD#%d : [ %4f | %4f | %4f | %4f ]",
            this.getIndex(), this.getYaw(), this.getPitch(), this.getRoll(), this.getThrottle()
        ); // Index, Yaw, Pitch, Roll, Throttle
    }

    public Command toEpisodeCommand() {
        Command command = new Command();
        command.index = this.getIndex();
        command.yaw = this.getYaw();
        command.pitch = this.getPitch();
        command.roll = this.getRoll();
        command.throttle = this.getThrottle();
        return command;
    }

    public DJIVirtualStickFlightControlData toDJIVirtualStickFlightControlData() {
        return new DJIVirtualStickFlightControlData(this.getPitch(), this.getRoll(), this.getYaw(), this.getThrottle());
    }
}

