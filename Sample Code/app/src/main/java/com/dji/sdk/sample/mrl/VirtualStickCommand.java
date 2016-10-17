package com.dji.sdk.sample.mrl;

import dji.common.flightcontroller.DJIFlightControllerDataType;

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
    public VirtualStickCommand() {
        this.setIndex(0);
        this.setPitch(0);
        this.setRoll(0);
        this.setYaw(0);
        this.setThrottle(0);
    }
    public VirtualStickCommand(int index, float pitch, float roll, float yaw, float throttle) {
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
}

