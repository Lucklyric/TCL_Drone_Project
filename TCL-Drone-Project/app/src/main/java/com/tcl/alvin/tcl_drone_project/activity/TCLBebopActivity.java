package com.tcl.alvin.tcl_drone_project.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
//import android.media.FaceDetector;
import android.os.Bundle;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.face.FaceDetector;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.tcl.alvin.tcl_drone_project.R;
import com.tcl.alvin.tcl_drone_project.controller.TCLInteliDroneController;
import com.tcl.alvin.tcl_drone_project.model.TCLBebopDrone;
import com.tcl.alvin.tcl_drone_project.controller.TCLGraphicFaceTrackerFactory;
import com.tcl.alvin.tcl_drone_project.model.TCLBebopHandler;

import com.tcl.alvin.tcl_drone_project.util.TCLNdkJniUtils;
import com.tcl.alvin.tcl_drone_project.view.TCLBebopVideoView;
import com.tcl.alvin.tcl_drone_project.view.TCLGraphicOverlay;

import java.io.ByteArrayOutputStream;


/**
 * Created by Alvin on 2016-05-25.
 */
public class TCLBebopActivity extends AppCompatActivity {
    private static final String TAG = "TCLBebopActivity";
    private TCLBebopDrone mBebopDrone;

    private ProgressDialog mConnectionProgressDialog;
    private ProgressDialog mDownloadProgressDialog;

    public TCLBebopVideoView mVideoView;

    private TextView mBatteryLabel;
    private Button mTakeOffLandBt;
    private Button mDownloadBt;
    private Button mAutoToggleBt;
    private int mNbMaxDownload;
    private int mCurrentDownloadIndex;
    private TCLGraphicOverlay mGraphicOverlay;
    private final Handler handler = new TCLBebopHandler(this);
    private TCLGraphicFaceTrackerFactory mGraphicFaceTrackerFactory = null;
    private FaceDetector detector = null; /*Using Google Vision API*/
    private TCLInteliDroneController droneController = null;
    private Boolean outPutFaceFlag = false;
    private Boolean ifAutoMode = false;

    static {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bebop);



        Intent intent = getIntent();
        ARDiscoveryDeviceService service = intent.getParcelableExtra(TCLDeviceListActivity.EXTRA_DEVICE_SERVICE);
        mBebopDrone = new TCLBebopDrone(this, service);
        mBebopDrone.addListener(mBebopListener);
        initIHM();
        mStatusChecker.run();

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        mVideoView.releaseMediaCodec();
    }


    @Override
    protected void onStart() {
        super.onStart();

        // show a loading view while the bebop drone is connecting
        if ((mBebopDrone != null) && !(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mBebopDrone.getConnectionState())))
        {
            mConnectionProgressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
            mConnectionProgressDialog.setIndeterminate(true);
            mConnectionProgressDialog.setMessage("Connecting ...");
            mConnectionProgressDialog.setCancelable(false);
            mConnectionProgressDialog.show();

            // if the connection to the Bebop fails, finish the activity
            if (!mBebopDrone.connect()) {
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mBebopDrone != null)
        {
            mConnectionProgressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
            mConnectionProgressDialog.setIndeterminate(true);
            mConnectionProgressDialog.setMessage("Disconnecting ...");
            mConnectionProgressDialog.setCancelable(false);
            mConnectionProgressDialog.show();

            if (!mBebopDrone.disconnect()) {
                finish();
            }
        }
    }

    private void configureOverLay(){
        mGraphicOverlay.setCameraInfo(mVideoView.VIDEO_WIDTH, mVideoView.VIDEO_HEIGHT,0);
        mGraphicOverlay.clear();
    }

    private void initIHM() {
        /*Init self bebop video view*/
        mVideoView = (TCLBebopVideoView) findViewById(R.id.videoView);

        /*Init overlay view*/
        mGraphicOverlay = (TCLGraphicOverlay) findViewById(R.id.faceOverlay);
        configureOverLay();
        Context context = getApplicationContext();

        /*Create the face detector*/
        detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(true)
                .setLandmarkType(FaceDetector.NO_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                .build();

        /*Create the custom factory processor*/
        mGraphicFaceTrackerFactory = new TCLGraphicFaceTrackerFactory(mGraphicOverlay);

        /*Create the multi processor*/
        detector.setProcessor(
                (new MultiProcessor.Builder<>(mGraphicFaceTrackerFactory)
                        .build()));

        /*Create the inteli controller*/
        droneController = new TCLInteliDroneController(mVideoView.VIDEO_HEIGHT,mVideoView.VIDEO_WIDTH,mGraphicFaceTrackerFactory,mBebopDrone);
        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        findViewById(R.id.emergencyBt).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBebopDrone.emergency();
            }
        });

        mTakeOffLandBt = (Button) findViewById(R.id.takeOffOrLandBt);
        mTakeOffLandBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switch (mBebopDrone.getFlyingState()) {
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                        mBebopDrone.takeOff();
                        break;
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                        mBebopDrone.land();
                        break;
                    default:
                }
            }
        });

        findViewById(R.id.takePictureBt).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBebopDrone.takePicture();
            }
        });

        mDownloadBt = (Button)findViewById(R.id.downloadBt);
        mDownloadBt.setEnabled(true);
        mDownloadBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//                mBebopDrone.getLastFlightMedias();
//
//                mDownloadProgressDialog = new ProgressDialog(TCLBebopActivity.this, R.style.AppCompatAlertDialogStyle);
//                mDownloadProgressDialog.setIndeterminate(true);
//                mDownloadProgressDialog.setMessage("Fetching medias");
//                mDownloadProgressDialog.setCancelable(false);
//                mDownloadProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        mBebopDrone.cancelGetLastFlightMedias();
//                    }
//                });
//                mDownloadProgressDialog.show();

                /*No need download function*/
                outPutFaceFlag = true;
            }
        });

        mAutoToggleBt = (Button)findViewById(R.id.autoToggle);
        mAutoToggleBt.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if (!ifAutoMode){
                    mAutoToggleBt.setText("Stop Auto");
                    ifAutoMode = true;
                }else{
                    mAutoToggleBt.setText("Start Auto");
                    ifAutoMode = false;
                }
            }
        });


        findViewById(R.id.gazUpBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setGaz((byte) 50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setGaz((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.gazDownBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setGaz((byte) -50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setGaz((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.yawLeftBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setYaw((byte) -50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setYaw((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.yawRightBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setYaw((byte) 50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setYaw((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.forwardBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setPitch((byte) 50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setPitch((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.backBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setPitch((byte) -50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setPitch((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.rollLeftBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setRoll((byte) -50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setRoll((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.rollRightBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setRoll((byte) 50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setRoll((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        mBatteryLabel = (TextView) findViewById(R.id.batteryLabel);
    }

    /**
     * Time out update function
     */
    private Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        /**
                         * Update the latest frame on the image view
                         */
                        if (mVideoView.ba != null){
//                            YuvImage yuvimage = new YuvImage(mVideoView.ba, ImageFormat.NV21, mVideoView.VIDEO_WIDTH, mVideoView.VIDEO_HEIGHT, null);
//                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                            yuvimage.compressToJpeg(new Rect(0, 0, mVideoView.VIDEO_WIDTH, mVideoView.VIDEO_HEIGHT), 100, baos);
//                            byte[] jdata = baos.toByteArray();
//                            Bitmap bmp = BitmapFactory.decodeByteArray(jdata,0,jdata.length);

                            Bitmap bmp = Bitmap.createBitmap(mVideoView.VIDEO_WIDTH, mVideoView.VIDEO_HEIGHT, Bitmap.Config.ARGB_8888);
                            bmp.setPixels(TCLNdkJniUtils.decodeYUV420SP(mVideoView.ba,mVideoView.VIDEO_WIDTH,mVideoView.VIDEO_HEIGHT),0,mVideoView.VIDEO_WIDTH,0,0,mVideoView.VIDEO_WIDTH,mVideoView.VIDEO_HEIGHT);
                           //TCLNdkJniUtils.naGetConvertedFrame(prBitmap,mVideoView.ba,mVideoView.VIDEO_WIDTH, mVideoView.VIDEO_HEIGHT);

//                            Bitmap mutableBitmap = bmp.copy(Bitmap.Config.RGB_565, true);
//                            int face_count = mFaceDetector.findFaces(mutableBitmap, faces);
//                            Log.d("Face_Detection", "Face Count: " + String.valueOf(face_count));
//                            Canvas canvas = new Canvas(mutableBitmap);
//
//                            for (int i = 0; i < face_count; i++) {
//                                FaceDetector.Face face = faces[i];
//                                tmp_paint.setColor(Color.RED);
//                                tmp_paint.setAlpha(100);
//                                face.getMidPoint(tmp_point);
//                                canvas.drawCircle(tmp_point.x, tmp_point.y, face.eyesDistance(),
//                                        tmp_paint);
//                            }
                            Frame frame = new Frame.Builder().setBitmap(bmp).build();

//                            SparseArray<Face> faces = detector.detect(frame);
//                            Bitmap mutableBitmap = bmp.copy(Bitmap.Config.RGB_565, true);
//                            Canvas canvas = new Canvas(mutableBitmap);
//                            for (int i = 0; i < faces.size(); ++i) {
//                                Face face = faces.valueAt(i);
//                                canvas.drawCircle(face.getPosition().x, face.getPosition().y, 10, tmp_paint);
//                            }
                            System.out.println("[TCL DEBUG]:Before send receive frame");
//                            mGraphicFaceTrackerFactory.resetFaces(); /*Clear face array*/
//                            detector.receiveFrame(frame); /*Feed frame to detector*/
//                            mGraphicFaceTrackerFactory.setmCurrentFrame(bmp); /*Set base frame of factory*/
//                            if (outPutFaceFlag){
//                                /*Output all faces to the phone storage*/
//                                mGraphicFaceTrackerFactory.saveAllFaces();
//                                outPutFaceFlag = false;
//                            }
//                            if (ifAutoMode){
//                                droneController.onUpdate();
//                            }
                            System.out.println("[TCL DEBUG]:After send receive frame");
                            mVideoView.setImageBitmap(bmp);
                        }
                    }
                }); //this function can change value of mInterval.
            } finally {
                handler.postDelayed(mStatusChecker, 1000/30);
            }
        }
    };

    /**
     * TCLBebopDrone callbacks
     */
    private final TCLBebopDrone.Listener mBebopListener = new TCLBebopDrone.Listener() {
        @Override
        public void onDroneConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
            switch (state)
            {
                case ARCONTROLLER_DEVICE_STATE_RUNNING:
                    mConnectionProgressDialog.dismiss();
                    break;

                case ARCONTROLLER_DEVICE_STATE_STOPPED:
                    // if the deviceController is stopped, go back to the previous activity
                    mConnectionProgressDialog.dismiss();
                    finish();
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onBatteryChargeChanged(int batteryPercentage) {
            mBatteryLabel.setText(String.format("%d%%", batteryPercentage));
        }

        @Override
        public void onPilotingStateChanged(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {
            switch (state) {
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                    mTakeOffLandBt.setText("Take off");
                    mTakeOffLandBt.setEnabled(true);
                   // mDownloadBt.setEnabled(true);
                    break;
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                    mTakeOffLandBt.setText("Land");
                    mTakeOffLandBt.setEnabled(true);
                   // mDownloadBt.setEnabled(false);
                    break;
                default:
                    mTakeOffLandBt.setEnabled(false);
                   // mDownloadBt.setEnabled(false);
            }
        }

        @Override
        public void onPictureTaken(ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error) {
            Log.i(TAG, "Picture has been taken");
        }

        @Override
        public void configureDecoder(ARControllerCodec codec) {
            mVideoView.configureDecoder(codec);
        }

        @Override
        public void onFrameReceived(ARFrame frame) {
            mVideoView.displayFrame(frame);
        }

        @Override
        public void onMatchingMediasFound(int nbMedias) {
            mDownloadProgressDialog.dismiss();

            mNbMaxDownload = nbMedias;
            mCurrentDownloadIndex = 1;

            if (nbMedias > 0) {
                mDownloadProgressDialog = new ProgressDialog(TCLBebopActivity.this, R.style.AppCompatAlertDialogStyle);
                mDownloadProgressDialog.setIndeterminate(false);
                mDownloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mDownloadProgressDialog.setMessage("Downloading medias");
                mDownloadProgressDialog.setMax(mNbMaxDownload * 100);
                mDownloadProgressDialog.setSecondaryProgress(mCurrentDownloadIndex * 100);
                mDownloadProgressDialog.setProgress(0);
                mDownloadProgressDialog.setCancelable(false);
                mDownloadProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //mBebopDrone.cancelGetLastFlightMedias();
                    }
                });
                mDownloadProgressDialog.show();
            }
        }

        @Override
        public void onDownloadProgressed(String mediaName, int progress) {
            mDownloadProgressDialog.setProgress(((mCurrentDownloadIndex - 1) * 100) + progress);
        }

        @Override
        public void onDownloadComplete(String mediaName) {
            mCurrentDownloadIndex++;
            mDownloadProgressDialog.setSecondaryProgress(mCurrentDownloadIndex * 100);

            if (mCurrentDownloadIndex > mNbMaxDownload) {
                mDownloadProgressDialog.dismiss();
                mDownloadProgressDialog = null;
            }
        }
    };
}