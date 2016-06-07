package com.tcl.alvin.tcl_drone_project.controller;

import android.widget.TextView;

import com.google.android.gms.vision.face.Face;
import com.tcl.alvin.tcl_drone_project.model.TCLBebopDrone;

/**
 * Created by Alvin on 2016-05-31.
 */

public class TCLInteliDroneController {
    public enum TCL_DRONE_CONTROLLER_STATUS {
        IDEL, NOFACE, TRACKING
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
    }


    public void onUpdate() {
        judgeAction();
    }

    public void judgeAction() {
        long tmpTimeStamp = System.currentTimeMillis();
        int droneFlag = 0;
        if (faceTrackerFactoryInstance.currentFaces().size() > 0) {
//            if ((tmpTimeStamp - mLastTimeStamp)<(1000*2)){
//                return;
//            }else{
//                mLastTimeStamp = tmpTimeStamp;
//            }
            mLastTimeStamp = tmpTimeStamp;
            mLastFrameHasFace = true;
            Face face = faceTrackerFactoryInstance.currentFaces().get(0);
            float x = (face.getPosition().x + face.getWidth() / 2);
            float y = (face.getPosition().y + face.getHeight() / 2);

            float centerX = mImageWidth / 2;
            float centerY = mImageHeight / 2;


            if ((face.getHeight()/mImageHeight)>0.6){
                droneInstance.setPitch((byte) 10);
                //droneInstance.setFlag((byte) 1);
                droneFlag = 1;
                mLogView.setText(mLastTimeStamp+"back");
                System.out.println("[TCL DEBUG]:back");


            }else if ((face.getHeight()/mImageHeight)<0.2){
                droneInstance.setPitch((byte) -10);
                droneFlag = 1;
                mLogView.setText(mLastTimeStamp+"forward");
                System.out.println("[TCL DEBUG]:forward");

            }else{
                droneInstance.setPitch((byte) 0);
                //droneInstance.setFlag((byte) 0);
            }

            if ((y - centerY) > 10) {
                droneInstance.setGaz((byte) -25);
                System.out.println("[TCL DEBUG]:Gaz DOWN");
                mLogView.setText(mLastTimeStamp+"GazDown");

            } else if ((y - centerY) < -10) {
                droneInstance.setGaz((byte) 25);
                System.out.println("[TCL DEBUG]:Gaz up");
                mLogView.setText(mLastTimeStamp+"Gazup");
            } else{
                droneInstance.setGaz((byte) 0);
                mLogView.setText(mLastTimeStamp+"Gza zero");
            }

            if ((x - centerX) > 50) {
                droneInstance.setRoll((byte) 10);
                droneFlag = 1;
                mLogView.setText(mLastTimeStamp+"Roll right");
                System.out.println("[TCL DEBUG]:Roll right");

            } else if ((x - centerX) < -50) {
                droneInstance.setRoll((byte) -10);
                droneFlag = 1;
                mLogView.setText(mLastTimeStamp+"Roll left");
                System.out.println("[TCL DEBUG]:Roll left");
            } else {
                droneInstance.setRoll((byte) 0);
                //droneInstance.setFlag((byte) 0);
                mLogView.setText(mLastTimeStamp+"Roll zero");
            }

            droneInstance.setFlag((byte) droneFlag);

        }else{
            if (mLastFrameHasFace){
                mLastFrameHasFace = false;
                droneInstance.setGaz((byte) 0);
                droneInstance.setRoll((byte) 0);
                droneInstance.setPitch((byte) 0);
                droneInstance.setFlag((byte) 0);
                mLogView.setText(mLastTimeStamp+"Empty");
                System.out.println("[TCL DEBUG]:Empty");

            }
        }
    }
}
