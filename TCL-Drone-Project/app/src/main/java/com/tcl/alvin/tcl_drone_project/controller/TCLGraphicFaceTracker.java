package com.tcl.alvin.tcl_drone_project.controller;

/**
 * Created by Alvin on 2016-05-30.
 */

import android.graphics.Bitmap;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.tcl.alvin.tcl_drone_project.view.TCLFaceGraphic;
import com.tcl.alvin.tcl_drone_project.view.TCLGraphicOverlay;

/**
 * Face tracker for each detected individual. This maintains a face graphic within the app's
 * associated face overlay.
 */
public class TCLGraphicFaceTracker extends Tracker<Face> {
    private TCLGraphicOverlay mOverlay;
    private TCLFaceGraphic mFaceGraphic;
    private TCLGraphicFaceTrackerFactory mFactorInstance;

    TCLGraphicFaceTracker(TCLGraphicOverlay overlay,TCLGraphicFaceTrackerFactory instance) {
        mOverlay = overlay;
        mFaceGraphic = new TCLFaceGraphic(overlay);
        mFactorInstance = instance;
    }

    /**
     * Start tracking the detected face instance within the face overlay.
     */
    @Override
    public void onNewItem(int faceId, Face item) {
        mFaceGraphic.setId(faceId);
    }

    /**
     * Update the position/characteristics of the face within the overlay.
     */
    @Override
    public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
        mOverlay.add(mFaceGraphic);
        mFaceGraphic.updateFace(face);
        mFactorInstance.enterFace(face);
        System.out.println("[TCL DEBUG]: update face position");
    }

    /**
     * Hide the graphic when the corresponding face was not detected.  This can happen for
     * intermediate frames temporarily (e.g., if the face was momentarily blocked from
     * view).
     */
    @Override
    public void onMissing(FaceDetector.Detections<Face> detectionResults) {
        mOverlay.remove(mFaceGraphic);
    }

    /**
     * Called when the face is assumed to be gone for good. Remove the graphic annotation from
     * the overlay.
     */
    @Override
    public void onDone() {
        mOverlay.remove(mFaceGraphic);
    }
}