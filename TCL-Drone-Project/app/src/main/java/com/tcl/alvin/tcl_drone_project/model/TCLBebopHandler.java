package com.tcl.alvin.tcl_drone_project.model;

import android.os.Handler;
import android.os.Message;

import com.tcl.alvin.tcl_drone_project.activity.TCLBebopActivity;

import java.lang.ref.WeakReference;

/**
 * Created by Alvin on 2016-05-25.
 */
public class TCLBebopHandler extends Handler {
    private final WeakReference<TCLBebopActivity> mActivity;

    public TCLBebopHandler(TCLBebopActivity activity) {
        mActivity = new WeakReference<TCLBebopActivity>(activity);
    }

    @Override
    public void handleMessage(Message msg) {

            super.handleMessage(msg);
    }
}