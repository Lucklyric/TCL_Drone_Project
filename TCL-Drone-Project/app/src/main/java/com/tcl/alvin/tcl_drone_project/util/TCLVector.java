package com.tcl.alvin.tcl_drone_project.util;

import android.graphics.Point;

/**
 * Created by Alvin on 2016-05-31.
 */
public class TCLVector {
    private int x = 0;
    private int y = 0;
    private int z = 0;

    public TCLVector(int x, int y){
        this.x = x;
        this.y = y;
    }

    public TCLVector(int x, int y,int z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setZ(int z) {
        this.z = z;
    }

}
