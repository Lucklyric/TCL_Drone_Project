package com.tcl.alvin.tcl_drone_project.util;

import android.graphics.Bitmap;

/**
 * Created by Alvin on 2016-06-02.
 */
public class TCLNdkJniUtils {
        public static native String getStringFormC();
        //public static native void naGetConvertedFrame(Bitmap _bitmap, byte [] _sourceFile, int _width, int _height);
        public static native int[] decodeYUV420SP(byte[] buf, int width, int height);
}
