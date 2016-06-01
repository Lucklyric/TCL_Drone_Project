package com.tcl.alvin.tcl_drone_project.controller;

import android.graphics.Point;

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

    public TCLInteliDroneController(int height, int width, TCLGraphicFaceTrackerFactory trackerFactory, TCLBebopDrone drone) {
        this.mStatus = TCL_DRONE_CONTROLLER_STATUS.IDEL;
        this.mImageHeight = height;
        this.mImageWidth = width;
        this.faceTrackerFactoryInstance = trackerFactory;
        this.droneInstance = drone;
    }


    public void onUpdate() {
        judgeAction();
    }

    public void judgeAction() {

        if (faceTrackerFactoryInstance.currentFaces().size() > 0) {
            Face face = faceTrackerFactoryInstance.currentFaces().get(0);
            float x = (face.getPosition().x + face.getWidth() / 2);
            float y = (face.getPosition().y + face.getHeight() / 2);

            float centerX = mImageWidth / 2;
            float centerY = mImageHeight / 2;

            if ((y - centerY) > mImageHeight / 10) {
                droneInstance.setGaz((byte) -50);
                System.out.println("[TCL DEBUG]:Gaz DOWN");
            } else if ((y - centerY) < -mImageHeight / 10) {
                droneInstance.setGaz((byte) 50);
                System.out.println("[TCL DEBUG]:Gaz up");

            }

            if ((x - centerX) > mImageWidth / 20) {
                droneInstance.setRoll((byte) -50);
                System.out.println("[TCL DEBUG]:Roll left");

            } else if ((x - centerX) < -mImageWidth / 20) {
                droneInstance.setRoll((byte) 50);
                System.out.println("[TCL DEBUG]:Roll right");

            }

        }

    }


}
