package com.tcl.alvin.tcl_drone_project.controller;

/**
 * Created by Alvin on 2016-05-30.
 */

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.tcl.alvin.tcl_drone_project.view.TCLGraphicOverlay;

/**
 * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
 * uses this factory to create face trackers as needed -- one for each individual.
 */
public class TCLGraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
    private TCLGraphicOverlay mGraphicOverlay;

    public TCLGraphicFaceTrackerFactory(TCLGraphicOverlay overlay){
        this.mGraphicOverlay = overlay;
    }
    @Override
    public Tracker<Face> create(Face face) {
        System.out.println("[TCL DEBUG]:Create face");
        return new TCLGraphicFaceTracker(mGraphicOverlay);
    }
}