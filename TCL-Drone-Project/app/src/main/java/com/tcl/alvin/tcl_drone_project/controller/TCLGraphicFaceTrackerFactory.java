package com.tcl.alvin.tcl_drone_project.controller;

/**
 * Created by Alvin on 2016-05-30.
 */

import android.graphics.Bitmap;
import android.util.SparseArray;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.tcl.alvin.tcl_drone_project.util.TCLPicUtil;
import com.tcl.alvin.tcl_drone_project.view.TCLGraphicOverlay;

import java.lang.reflect.Array;
import java.util.Vector;

/**
 * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
 * uses this factory to create face trackers as needed -- one for each individual.
 */
public class TCLGraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
    private TCLGraphicOverlay mGraphicOverlay;
    private Vector<Bitmap> mFacesBitmap = new Vector<>();
    private Vector<Face> mFaces = new Vector<>();
    private Bitmap mCurrentFrame;
    private int count = 0;

    public TCLGraphicFaceTrackerFactory(TCLGraphicOverlay overlay){
        this.mGraphicOverlay = overlay;
    }

    public void resetFaces(){
        this.mFacesBitmap.clear();
        this.mFaces.clear();
    }

    public void setmCurrentFrame(Bitmap currentFrame){
        this.mCurrentFrame = currentFrame;
    }

    public void saveAllFaces(){
        System.out.println("[TCL DEBUG]:Out put"+mFacesBitmap.size()+"Faces");
        for (int i = 0 ; i < mFacesBitmap.size(); ++i){
            TCLPicUtil.saveToCacheFile(mFacesBitmap.elementAt(i),String.valueOf(count));
            count++;
        }
    }
    public void enterFace(Face face){
        try{
            Bitmap tmp = Bitmap.createBitmap(this.mCurrentFrame,(int)face.getPosition().x,(int)face.getPosition().y,(int)face.getWidth(),(int)face.getHeight());
            mFacesBitmap.add(tmp);
            mFaces.add(face);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public Vector<Face> currentFaces(){
        return mFaces;
    }

    @Override
    public Tracker<Face> create(Face face) {
        System.out.println("[TCL DEBUG]:Create face");
        return new TCLGraphicFaceTracker(mGraphicOverlay,this);
    }
}