package com.example.ARCoreWithOpenCV;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "MainActivity";

    private Mat mRgba;
    private Mat mIntermediateMat;
    private Mat mGray;

    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    //limit resolution for faster processing
                    mOpenCvCameraView.setMaxFrameSize(640,480);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
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
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        //Useful when we don't want to view openCV output and just want to view arcore
       //mOpenCvCameraView.setAlpha(0);


        mOpenCvCameraView.setCvCameraViewListener(this);

    }


    @Override
    public void onResume()
    {
        super.onResume();
        Log.i(TAG,"Called onResume()");
        if (!OpenCVLoader.initDebug()) {
            //Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
        } else {
            //Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
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
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        Log.i(TAG,"Called onCameraViewStarted()");

    }

    public void onCameraViewStopped() {
        Log.i(TAG,"Called onCameraViewStopped()");
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.i(TAG,"Called onCameraFrame()");

        //Get a unmodified copy of the original frame
        Mat originalFrame = inputFrame.rgba().clone();

        //apply gaussian blur on frame, then gray it
        Mat blurFrame = inputFrame.rgba().clone();
        Size gaussianKernel = new Size(7,7);
        Imgproc.GaussianBlur(inputFrame.rgba(),blurFrame, gaussianKernel,1);
        Mat grayBlurFrame = inputFrame.rgba().clone();
        Imgproc.cvtColor(blurFrame, grayBlurFrame, Imgproc.COLOR_RGB2GRAY);

        //what about median blur?


        //apply canny edge
        Mat edgesFrame = inputFrame.gray().clone();
        Imgproc.Canny(grayBlurFrame, edgesFrame,23,83);

        //apply dilation
        Mat dilationKernel = Mat.ones(5,5,1);
        Mat dilatedFrame = inputFrame.gray().clone();
        Point anchor = new Point(-1,-1);
        Imgproc.dilate(edgesFrame,dilatedFrame,dilationKernel,anchor,1);
        List<MatOfPoint> contours = getContours(dilatedFrame);

        //draw contours
        drawContours(contours,originalFrame);

        return originalFrame;


    }

    public List<MatOfPoint> getContours(Mat frame){
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(frame,contours, hierarchy, Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_NONE);
        return contours;
    }

    public void drawContours(List<MatOfPoint> contours,Mat frame){
        Scalar color = new Scalar(0,255,0);
        for (int i = 0; i < contours.size(); i++){
            double area = Imgproc.contourArea(contours.get(i));
            if (area >10000) {
                Log.i(TAG, String.valueOf(area));
                Imgproc.drawContours(frame, contours,-1, color,3);
            }
        }

    }
}