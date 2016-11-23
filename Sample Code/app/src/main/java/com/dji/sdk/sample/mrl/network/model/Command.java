package com.dji.sdk.sample.mrl.network.model;


import com.dji.sdk.sample.mrl.VirtualStickCommand;

public class Command {
    public Double t;
    public Float dx;
    public Float dy;
    public Float dz;
    public Float drz;

    // Coordinate System Conversion : global x, y, z to roll, pitch, yaw
    public VirtualStickCommand toVirtualStickCommand() {
        return new VirtualStickCommand(-this.dy, this.dx, -this.drz, -this.dz); // pitch, roll, yaw, throttle
    }
}
