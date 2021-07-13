package com.example.ARCoreWithOpenCV;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.ARCoreWithOpenCV.common.helpers.CameraPermissionHelper;
import com.example.ARCoreWithOpenCV.utils.ImageProcessing;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
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

    private final ImageProcessing imageProcessing = new ImageProcessing(this);


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
            //Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
        } else {
            //Log.d(TAG, "OpenCV library found inside package. Using it!");
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
        //load image to animate with
        try {
            Mat overlayImage = Utils.loadResource(this, R.drawable.animeface, CvType.CV_8UC4); //with alpha channel
            Mat maskImage = imageProcessing.getPngMask(overlayImage);
            ImageProcessing.setOverlayImage(overlayImage);
            imageProcessing.setMaskImage(maskImage);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onCameraViewStopped() {
        Log.i(TAG,"Called onCameraViewStopped()");
    }

    //carry out frame processing here
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.i(TAG,"Called onCameraFrame()");

        //Get a unmodified rgb copy of the original frame
        Mat originalFrame = inputFrame.rgba().clone();
        Imgproc.cvtColor(originalFrame, originalFrame,Imgproc.COLOR_RGBA2RGB);

        //apply gaussian blur on frame, then convert it to grayscale
        Mat blurFrame = originalFrame.clone();
        Size gaussianKernel = new Size(7,7);
        Imgproc.GaussianBlur(originalFrame,blurFrame, gaussianKernel,1);
        Imgproc.cvtColor(blurFrame, blurFrame, Imgproc.COLOR_RGB2GRAY);

        //can also apply median blur
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
        imageProcessing.drawContours(contours,originalFrame);

        return originalFrame;
    }

    }

