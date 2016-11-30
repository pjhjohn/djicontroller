package com.dji.sdk.sample.mrl.network.model;


import com.dji.sdk.sample.mrl.VirtualStickCommand;

public class Command {
    public Double t;
    public Float dx;
    public Float dy;
    public Float dz;
    public Float drz;

    public Command(double t, float dx, float dy, float dz, float drz) {
        this.t = t;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.drz = drz;
    }

    // Coordinate System Conversion : global x, y, z to roll, pitch, yaw
    public VirtualStickCommand toVirtualStickCommand() {
        return new VirtualStickCommand(-this.dy, this.dx, -this.drz, this.dz); // pitch, roll, yaw, throttle
    }
}
