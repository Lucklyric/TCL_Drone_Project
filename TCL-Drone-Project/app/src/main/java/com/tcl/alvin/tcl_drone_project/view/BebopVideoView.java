package com.tcl.alvin.tcl_drone_project.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.FaceDetector;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.parrot.arsdk.arcontroller.ARCONTROLLER_STREAM_CODEC_TYPE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Handler;

/**
 * Created by Alvin on 2016-05-25.
 */
public class BebopVideoView extends ImageView{
    private static final String TAG = "BebopVideoView";
    private static final String VIDEO_MIME_TYPE = "video/avc";
    private static final int VIDEO_DEQUEUE_TIMEOUT = 33000;
    private ImageReader mReader = null;
    private MediaCodec mMediaCodec;
    private Lock mReadyLock;

    private boolean mIsCodecConfigured = false;
    private ByteBuffer mSpsBuffer;
    private ByteBuffer mPpsBuffer;

    private ByteBuffer[] mBuffers;
    public Bitmap lastFrame = null;
    public Handler mHandler = null;
    private static final int VIDEO_WIDTH = 640;
    private static final int VIDEO_HEIGHT = 368;
    private FaceDetector mFaceDetector = new FaceDetector(VIDEO_WIDTH,VIDEO_HEIGHT,10);
    private FaceDetector.Face[] faces = new FaceDetector.Face[10];

    public BebopVideoView(Context context) {
        super(context);
        customInit();
    }

    public BebopVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        customInit();
    }

    public BebopVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        customInit();
    }


    private void customInit() {
        mReadyLock = new ReentrantLock();
        //setWillNotDraw(false);
        //getHolder().addCallback(this);
        mReadyLock.lock();
        initMediaCodec(VIDEO_MIME_TYPE);
        mReadyLock.unlock();
    }

    public void displayFrame(ARFrame frame) {
        mReadyLock.lock();
        //System.out.println("[TCL DEBUG:]"+frame.isIFrame()+"size"+frame.getDataSize());

        if ((mMediaCodec != null)) {
            if (mIsCodecConfigured) {
                // Here we have either a good PFrame, or an IFrame
                int index = -1;

                try {
                    index = mMediaCodec.dequeueInputBuffer(VIDEO_DEQUEUE_TIMEOUT);
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Error while dequeue input buffer");
                }
                if (index >= 0) {
                    ByteBuffer b;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        b = mMediaCodec.getInputBuffer(index);

                    } else {
                        b = mBuffers[index];
                        b.clear();
                    }

                    if (b != null) {
                        b.put(frame.getByteData(), 0, frame.getDataSize());
                    }

                    try {
                        mMediaCodec.queueInputBuffer(index, 0, frame.getDataSize(), 0, 0);
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "Error while queue input buffer");
                    }
                }
            }
/**
 * TODO Get a next released buffered frame and process with face detector,then modify it and put it back to the outputbuffer.
 */
            // Try to display previous frame
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int outIndex;
            try {
                outIndex = mMediaCodec.dequeueOutputBuffer(info, 0);
                while (outIndex >= 0) {
                    //ByteBuffer[] outputbuffers = mMediaCodec.getOutputBuffers();
                    ByteBuffer buffer = mMediaCodec.getOutputBuffer(outIndex); //The bytebuffer i want to convert to bitmap
                    buffer.position(info.offset);
                    buffer.limit(info.offset + info.size);
                    byte[] ba = new byte[buffer.remaining()];
                    buffer.get(ba);
                    //System.out.println("[TCL DEBUG:]"+ba[1]);

                    YuvImage yuvimage = new YuvImage(ba, ImageFormat.NV21, VIDEO_WIDTH, VIDEO_HEIGHT, null);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    //System.out.println("[TCL DEBUG:]"+yuvimage.getYuvData().length);
                    yuvimage.compressToJpeg(new Rect(0, 0, VIDEO_WIDTH, VIDEO_HEIGHT), 10, baos);
                    byte[] jdata = baos.toByteArray();
                    Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);
                    Bitmap mutableBitmap = bmp.copy(Bitmap.Config.RGB_565, true);
                    int facecount = mFaceDetector.findFaces(mutableBitmap, faces);
                    System.out.println("[TCL DEBUG:]Face found"+facecount);
                    lastFrame = bmp;
                    mMediaCodec.releaseOutputBuffer(outIndex, false);
                    outIndex = mMediaCodec.dequeueOutputBuffer(info, 0);
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error while dequeue input buffer (outIndex)");
            }
        }
        mReadyLock.unlock();
    }

    public void configureDecoder(ARControllerCodec codec) {
        mReadyLock.lock();

        if (codec.getType() == ARCONTROLLER_STREAM_CODEC_TYPE_ENUM.ARCONTROLLER_STREAM_CODEC_TYPE_H264) {
            ARControllerCodec.H264 codecH264 = codec.getAsH264();

            mSpsBuffer = ByteBuffer.wrap(codecH264.getSps().getByteData());
            mPpsBuffer = ByteBuffer.wrap(codecH264.getPps().getByteData());
        }

        if ((mMediaCodec != null) && (mSpsBuffer != null)) {
            configureMediaCodec();
        }

        mReadyLock.unlock();
    }

    private void configureMediaCodec() {
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT);
        format.setByteBuffer("csd-0", mSpsBuffer);
        format.setByteBuffer("csd-1", mPpsBuffer);

        //mMediaCodec.configure(format, getHolder().getSurface(), null, 0);
        mMediaCodec.configure(format, null, null, 0);
        mMediaCodec.start();

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            mBuffers = mMediaCodec.getInputBuffers();
        }

        mIsCodecConfigured = true;
    }

    private void initMediaCodec(String type) {
        try {
            mMediaCodec = MediaCodec.createDecoderByType(type);
        } catch (IOException e) {
            Log.e(TAG, "Exception", e);
        }

        if ((mMediaCodec != null) && (mSpsBuffer != null)) {
            configureMediaCodec();
        }
    }

    public void releaseMediaCodec() {
        if (mMediaCodec != null) {
            if (mIsCodecConfigured) {
                mMediaCodec.stop();
                mMediaCodec.release();
            }
            mIsCodecConfigured = false;
            mMediaCodec = null;
        }
    }


}
