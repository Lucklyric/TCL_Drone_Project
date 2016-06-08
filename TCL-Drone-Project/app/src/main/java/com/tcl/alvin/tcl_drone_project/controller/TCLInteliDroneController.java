package com.tcl.alvin.tcl_drone_project.controller;

import android.widget.TextView;

import com.google.android.gms.vision.face.Face;
import com.tcl.alvin.tcl_drone_project.model.TCLBebopDrone;

/**
 * Created by Alvin on 2016-05-31.
 */

public class TCLInteliDroneController {
    public enum TCL_DRONE_CONTROLLER_STATUS {
        IDEL, NOFACE, SEARCHING
    }

    public enum TCL_DRONE_CONTROLLER_ACTION {
        NOTHING
    }

    private TCL_DRONE_CONTROLLER_STATUS mStatus;
    private int mImageHeight;
    private int mImageWidth;
    private TCLGraphicFaceTrackerFactory faceTrackerFactoryInstance;
    private TCLBebopDrone droneInstance;
    private long mLastTimeStamp;
    private boolean mLastFrameHasFace;
    private long mTimeLostFace;
    private TextView mLogView;

    public TCLInteliDroneController(int height, int width, TCLGraphicFaceTrackerFactory trackerFactory, TCLBebopDrone drone, TextView logView) {
        this.mStatus = TCL_DRONE_CONTROLLER_STATUS.IDEL;
        this.mImageHeight = height;
        this.mImageWidth = width;
        this.faceTrackerFactoryInstance = trackerFactory;
        this.droneInstance = drone;
        this.mLastTimeStamp = System.currentTimeMillis();
        this.mLastFrameHasFace = false;
        this.mLogView = logView;
        this.mTimeLostFace = 0;
    }

    public void onUpdate() {
        judgeAction();
    }

    public void judgeAction() {
        long tmpTimeStamp = System.currentTimeMillis();
        int droneFlag = 0;
        if (faceTrackerFactoryInstance.currentFaces().size() > 0) {

            /**
             * Check if the drone is in searching state
             */
            if (mStatus == TCL_DRONE_CONTROLLER_STATUS.SEARCHING) {
                droneInstance.setYaw((byte) 0);
                droneFlag = 1;
            }

            mLastTimeStamp = tmpTimeStamp;
            mLastFrameHasFace = true;
            Face face = faceTrackerFactoryInstance.currentFaces().get(0);
            float x = (face.getPosition().x + face.getWidth() / 2);
            float y = (face.getPosition().y + face.getHeight() / 2);

            float centerX = mImageWidth / 2;
            float centerY = mImageHeight / 2;

            float deltaX = x - centerX;
            float deltaY = y - centerY;


            /**
             * Core controlling part
             */
            double weight = 0.26;
            if ((deltaY) > 2) {
                droneInstance.setGaz((byte) -(deltaY * weight));
                System.out.println("[TCL DEBUG]:Gaz DOWN");
                mLogView.setText(mLastTimeStamp + "GazDown");

            } else if ((deltaY) < -2) {
                droneInstance.setGaz((byte) -(deltaY * weight));
                System.out.println("[TCL DEBUG]:Gaz up");
                mLogView.setText(mLastTimeStamp + "Gazup");
            } else {
                droneInstance.setGaz((byte) 0);
                mLogView.setText(mLastTimeStamp + "Gza zero");
            }

            if ((deltaX) > 2) {
                droneInstance.setYaw((byte) (deltaX * weight));
                mLogView.setText(mLastTimeStamp + "Yaw right");
                System.out.println("[TCL DEBUG]:Roll right");

            } else if (deltaX < -2) {
                droneInstance.setYaw((byte) (deltaX * weight));
                mLogView.setText(mLastTimeStamp + "Yaw left");
                System.out.println("[TCL DEBUG]:Roll left");
            } else {
                droneInstance.setYaw((byte) 0);
                mLogView.setText(mLastTimeStamp + "Yaw zero");
            }

            double deltaSizeLow = face.getHeight() - mImageHeight * 0.40;
            double deltaSizeHigh = face.getHeight() - mImageHeight * 0.25;

            if (deltaSizeLow > 2) {
                droneInstance.setPitch((byte) (-6));
                droneFlag = 1;
                mLogView.setText(mLastTimeStamp + "back" + deltaSizeLow);
                System.out.println("[TCL DEBUG]:back");

            } else if (deltaSizeHigh < -2) {
                droneInstance.setPitch((byte) (6));
                droneFlag = 1;
                mLogView.setText(mLastTimeStamp + "forward" + deltaSizeHigh);
                System.out.println("[TCL DEBUG]:forward");

            } else {
                droneInstance.setPitch((byte) 0);
            }

            droneInstance.setFlag((byte) droneFlag);
        } else {
            if (mLastFrameHasFace) {
                mStatus = TCL_DRONE_CONTROLLER_STATUS.NOFACE;
                mLastFrameHasFace = false;
                mTimeLostFace = tmpTimeStamp;
                droneInstance.setGaz((byte) 0);
                droneInstance.setYaw((byte) 0);
                //droneInstance.setRoll((byte) 0);
                droneInstance.setPitch((byte) 0);
                droneInstance.setFlag((byte) 0);
                mLogView.setText(mLastTimeStamp + "Empty");
                System.out.println("[TCL DEBUG]:Empty");
            }

            if (mStatus == TCL_DRONE_CONTROLLER_STATUS.NOFACE) {
                if ((tmpTimeStamp - mTimeLostFace) > (1000 * 3)) {
                    mLogView.setText(mTimeLostFace + "Searching");
                    mStatus = TCL_DRONE_CONTROLLER_STATUS.SEARCHING;
                    droneInstance.setYaw((byte) 10);
                    droneInstance.setFlag((byte) 1);
                }
            }
        }
    }
}
