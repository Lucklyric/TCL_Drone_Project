//
// Created by Alvin on 2016-06-03.
//
#include "com_tcl_alvin_tcl_drone_project_util_TCLNdkJniUtils.h"
#include <android/log.h>
#include <android/bitmap.h>
/*standard library*/
#include <time.h>
#include <math.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include <unistd.h>
#include <assert.h>
//#include "yuv2rgb/yuv2rgb.h"

#define LOG_TAG "yuv2rgb test"
#define LOG_LEVEL 10
#define LOGI(level, ...) if (level <= LOG_LEVEL) {__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);}
#define LOGE(level, ...) if (level <= LOG_LEVEL) {__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__);}


/*
 * Class:     com_tcl_alvin_tcl_drone_project_util_TCLNdkJniUtils
 * Method:    getStringFormC
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_tcl_alvin_tcl_1drone_1project_util_TCLNdkJniUtils_getStringFormC
        (JNIEnv * env, jclass obj){
    return (*env)->NewStringUTF(env,"这里是来自c的string");
}

int g_v_table[256],g_u_table[256],y_table[256];
int r_yv_table[256][256],b_yu_table[256][256];
int inited = 0;

void initTable()
{
    if (inited == 0)
    {
        inited = 1;
        int m = 0,n=0;
        for (; m < 256; m++)
        {
            g_v_table[m] = 833 * (m - 128);
            g_u_table[m] = 400 * (m - 128);
            y_table[m] = 1192 * (m - 16);
        }
        int temp = 0;
        for (m = 0; m < 256; m++)
            for (n = 0; n < 256; n++)
            {
                temp = 1192 * (m - 16) + 1634 * (n - 128);
                if (temp < 0) temp = 0; else if (temp > 262143) temp = 262143;
                r_yv_table[m][n] = temp;

                temp = 1192 * (m - 16) + 2066 * (n - 128);
                if (temp < 0) temp = 0; else if (temp > 262143) temp = 262143;
                b_yu_table[m][n] = temp;
            }
    }
}

/*
 * Class:     com_tcl_alvin_tcl_drone_project_util_TCLNdkJniUtils
 * Method:    decodeYUV420SP
 * Signature: ([BII)[I
 */
JNIEXPORT jintArray JNICALL Java_com_tcl_alvin_tcl_1drone_1project_util_TCLNdkJniUtils_decodeYUV420SP
        (JNIEnv * env, jclass obj, jbyteArray buf, jint width, jint height){
    if (!buf){
        return (*env)->NewIntArray(env, 0);
    }
    jbyte * yuv420sp = (*env)->GetByteArrayElements(env, buf, 0);


    int frameSize = width * height;
    jint rgb[frameSize];

    initTable();

    int i = 0, j = 0,yp = 0;
    int uvp = 0, u = 0, v = 0;
    for (j = 0, yp = 0; j < height; j++)
    {
        uvp = frameSize + (j >> 1) * width;
        u = 0;
        v = 0;
        for (i = 0; i < width; i++, yp++)
        {
            int y = (0xff & ((int) yuv420sp[yp]));
            if (y < 0)
                y = 0;
            if ((i & 1) == 0)
            {
                v = (0xff & yuv420sp[uvp++]);
                u = (0xff & yuv420sp[uvp++]);
            }

            int y1192 = y_table[y];
            int r = r_yv_table[y][v];
            int g = (y1192 - g_v_table[v] - g_u_table[u]);
            int b = b_yu_table[y][u];

            if (g < 0) g = 0; else if (g > 262143) g = 262143;

            rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
        }
    }

    jintArray result = (*env)->NewIntArray(env, frameSize);
    (*env)->SetIntArrayRegion(env, result, 0, frameSize, rgb);
    (*env)->ReleaseByteArrayElements(env, buf, yuv420sp, 0);
    return result;
}


//JNIEXPORT void JNICALL Java_com_tcl_alvin_tcl_1drone_1project_util_TCLNdkJniUtils_naGetConvertedFrame
//(JNIEnv * env, jclass obj, jobject pBitmap, jbyteArray pByteArray, jint _width, jint _height){
//    int lRet;
//    AndroidBitmapInfo lInfo;
//    void* l_Bitmap;
//    unsigned char y[_width*_height];
//    unsigned char u[_width*_height>>2];
//    unsigned char v[_width*_height>>2];
//    //1. retrieve information about the bitmap
//    if ((lRet = AndroidBitmap_getInfo(env, pBitmap, &lInfo)) < 0) {
//        LOGE(1, "AndroidBitmap_getInfo failed! error = %d", lRet);
//        return;
//    }
//
//    if (lInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
//        LOGE(1, "Bitmap format is not RGBA_8888!");
//        return;
//    }
//
//    //2. lock the pixel buffer and retrieve a pointer to it
//    if ((lRet = AndroidBitmap_lockPixels(env, pBitmap, &l_Bitmap)) < 0) {
//        LOGE(1, "AndroidBitmap_lockPixels() failed! error = %d", lRet);
//    }
//
//(*env)->GetByteArrayRegion (env,y, 0, _width*_height, (jbyte*)(pByteArray));
//(*env)->GetByteArrayRegion (env,u, _width*_height, _width*_height>>2, (jbyte*)(pByteArray));
//(*env)->GetByteArrayRegion (env,v, _width*_height, _width*_height>>2, (jbyte*)(pByteArray));
//
//    //3. do yuv->rgb conversion and fill in the data
//    LOGI(1, "start color conversion");
//    yuv420_2_rgb8888(l_Bitmap,
//        y,
//        v,
//        u,
//        _width,												//width
//        _height, 											//height
//        _width,												//Y span/pitch: No. of bytes in a row
//        _width>>1,											//UV span/pitch
//        _width<<2,											//bitmap span/pitch
//        yuv2rgb565_table,
//        0
//        );
//    LOGI(1, "end color conversion");
//    //4. unlock the bitmap
//    AndroidBitmap_unlockPixels(env, pBitmap);
//    return;
//}