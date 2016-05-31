package com.tcl.alvin.tcl_drone_project.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by Alvin on 2016-05-31.
 */
public class TCLPicUtil {
    public static File getSavePath() {
        File path;
        if (hasSDCard()) { // SD card
            path = new File(getSDCardPath() + "/TCL-Drone-FaceSamples/");
            path.mkdir();
        } else {
            path = Environment.getDataDirectory();
        }
        return path;
    }
    public static String getCacheFilename(String filename) {
        File f = getSavePath();
        return f.getAbsolutePath() + "/face"+filename+".png";
    }

    public static Bitmap loadFromFile(String filename) {
        try {
            File f = new File(filename);
            if (!f.exists()) { return null; }
            Bitmap tmp = BitmapFactory.decodeFile(filename);
            return tmp;
        } catch (Exception e) {
            return null;
        }
    }
    public static Bitmap loadFromCacheFile() {
        return loadFromFile(getCacheFilename("test"));
    }
    public static void saveToCacheFile(Bitmap bmp,String filename) {
        saveToFile(getCacheFilename(filename),bmp);
    }
    public static void saveToFile(String filename,Bitmap bmp) {
        System.out.println("[TCL DEBUG]:Going to output file");

        try {
            FileOutputStream out = new FileOutputStream(filename);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            System.out.println("[TCL DEBUG]:File output successed");
        } catch(Exception e) {}
    }

    public static boolean hasSDCard() { // SD????????
        String status = Environment.getExternalStorageState();
        return status.equals(Environment.MEDIA_MOUNTED);
    }
    public static String getSDCardPath() {
        File path = Environment.getExternalStorageDirectory();
        return path.getAbsolutePath();
    }
}
