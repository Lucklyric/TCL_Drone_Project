package com.tcl.alvin.tcl_drone_project.model;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;

import com.tcl.alvin.tcl_drone_project.activity.BebopActivity;

import java.lang.ref.WeakReference;

/**
 * Created by Alvin on 2016-05-25.
 */
public class MyHandler extends Handler {
    private final WeakReference<BebopActivity> mActivity;

    public MyHandler(BebopActivity activity) {
        mActivity = new WeakReference<BebopActivity>(activity);
    }

    @Override
    public void handleMessage(Message msg) {

            super.handleMessage(msg);
    }
}