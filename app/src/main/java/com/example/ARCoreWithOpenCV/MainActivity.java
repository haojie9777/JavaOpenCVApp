package com.example.ARCoreWithOpenCV;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.ARCoreWithOpenCV.Models.Gastly;
import com.example.ARCoreWithOpenCV.common.helpers.CameraPermissionHelper;
import com.example.ARCoreWithOpenCV.utils.ImageProcessing;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.List;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MainActivity";
    private CameraBridgeViewBase mOpenCvCameraView;

    //store coordinates of where user last touched the screen
    private final Point lastTouchCoordinates = new Point(-1,-1);
    private final ImageProcessing imageProcessing = new ImageProcessing();


    private static int viewMode = 0; //1 for debug mode, 0 for normal mode

    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    //limit resolution for faster processing
                    mOpenCvCameraView.setMaxFrameSize(640, 480);
                }
            }
        }
    };

    public MainActivity() throws IOException {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (OpenCVLoader.initDebug()){
            Log.d(TAG,"OpenCV loaded successfully");
        }
        else{
            Log.d(TAG,"OpenCV loading failed");
        }
        Log.i(TAG, "called onCreate");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.debug_button:
                MyApplication.toggleViewMode();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.i(TAG,"Called onResume()");

        // Ask for permission to use camera
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this);
            return;
        }
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }


    @Override
    public void onPause()
    {
        super.onPause();
        Log.i(TAG,"Called onPause()");
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"Called onDestroy()");
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    //implementation of CameraBridgeViewBase.CvCameraViewListener2 interface methods
    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG,"Called onCameraViewStarted()");
    }

    public void onCameraViewStopped() {
        Log.i(TAG,"Called onCameraViewStopped()");
    }

    //carry out frame processing here
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        //Get a unmodified rgb copy of the original frame
        Mat originalFrame = inputFrame.rgba().clone();
        Imgproc.cvtColor(originalFrame, originalFrame,Imgproc.COLOR_RGBA2RGB);

        //apply gaussian blur on frame, then convert it to grayscale
        Mat blurFrame = originalFrame.clone();
        Size gaussianKernel = new Size(7,7);
        Imgproc.GaussianBlur(originalFrame,blurFrame, gaussianKernel,1);
        Imgproc.cvtColor(blurFrame, blurFrame, Imgproc.COLOR_RGB2GRAY);

        //apply canny edge
        Imgproc.Canny(blurFrame, blurFrame,23,83); //more accurate edges

        //apply dilation
        Mat dilationKernel = Mat.ones(5,5,1);
        Point anchor = new Point(-1,-1);
        Imgproc.dilate(blurFrame,blurFrame,dilationKernel,anchor,1);

        //perform thresholding to remove noises
        Imgproc.threshold(blurFrame, blurFrame,127,255,Imgproc.THRESH_BINARY);

        //Get contours and draw them
        List<MatOfPoint> contours = imageProcessing.getContours(blurFrame);

        //convert to bgr for easier processing in opencv
        Imgproc.cvtColor(originalFrame,originalFrame,Imgproc.COLOR_RGB2BGR);
        if (lastTouchCoordinates.x != -1){
            boolean result = imageProcessing.processContours(contours,originalFrame,lastTouchCoordinates);
            if (!result){
                lastTouchCoordinates.x = -1;
                lastTouchCoordinates.y = -1;
            }

        }
        return originalFrame;


    }

    //Gets coordinate of user last touched point, convert to openCV 640x480, then stored it in memory
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Log.i(TAG, "touched screen!");
            DisplayMetrics dm = getResources().getDisplayMetrics();
            double x = event.getX();
            double y = event.getY();
            double correctedX = (x*640)/dm.widthPixels;
            double correctedY = (y*480)/dm.heightPixels;
            //store coordinate into variable
            double[] point = {correctedX,correctedY};
            lastTouchCoordinates.set(point);
            Log.i(TAG, lastTouchCoordinates.toString());

            //play sound if touched a figurine
            if (Gastly.coordinatesInBox(lastTouchCoordinates)){
                MediaPlayer ring= MediaPlayer.create(MainActivity.this,R.raw.gastly);
                ring.start();
            }


        }
        return true;
    }


}

