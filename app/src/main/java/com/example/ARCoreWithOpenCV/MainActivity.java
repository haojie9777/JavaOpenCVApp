package com.example.ARCoreWithOpenCV;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.ARCoreWithOpenCV.common.helpers.CameraPermissionHelper;
import com.google.ar.core.ArCoreApk;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "MainActivity";
    private static String CONTOURTAG = "ContourSize";

    // requestInstall(Activity, true) will triggers installation of
// Google Play Services for AR if necessary.
    private boolean mUserRequestedInstall = true;


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

        // Enable AR-related functionality on ARCore supported devices only.
        maybeEnableArButton();

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

    void maybeEnableArButton() {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
        if (availability.isTransient()) {
            // Continue to query availability at 5Hz while compatibility is checked in the background.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    maybeEnableArButton();
                }
            }, 200);
        }
        /*
        if (availability.isSupported()) {

            mArButton.setVisibility(View.VISIBLE);
            mArButton.setEnabled(true);
        } else { // The device is unsupported or unknown.
            mArButton.setVisibility(View.INVISIBLE);
            mArButton.setEnabled(false);
        }*/
    }



    @Override
    public void onResume()
    {
        super.onResume();
        Log.i(TAG,"Called onResume()");

        // ARCore requires camera permission to operate.
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
        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
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

        //perform thresholding to remove noises
        Mat thresholdFrame =  inputFrame.gray().clone();
        Imgproc.threshold(dilatedFrame, thresholdFrame,127,255,Imgproc.THRESH_BINARY);

        List<MatOfPoint> contours = getContours(thresholdFrame);
        //List<MatOfPoint> contours = getContours(dilatedFrame);

        //draw contours
        drawContours(contours,originalFrame);

        return originalFrame;


    }

    //Get list of all contours in the frame
    public List<MatOfPoint> getContours(Mat frame){
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(frame,contours, hierarchy, Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_NONE);
        return contours;
    }

    public void drawContours(List<MatOfPoint> contours,Mat frame){
        Scalar contourColor = new Scalar(0,255,0);
        Scalar boundingBoxColor = new Scalar(255,255,0);

        for (int i = 0; i < contours.size(); i++){
            double area = Imgproc.contourArea(contours.get(i));

            if (area >10000) {

                //get stats about contour's area, perimeter and vertices to predict shape
                MatOfPoint2f contour = new MatOfPoint2f(contours.get(i).toArray());
                double perimeter = Imgproc.arcLength(contour,true);
                MatOfPoint2f approxCurve = new MatOfPoint2f();
                Imgproc.approxPolyDP(contour,approxCurve,0.02*perimeter,true);
                int vertices = approxCurve.toArray().length;
                Rect rect = Imgproc.boundingRect(contours.get(i));
                double x = rect.x; double y = rect.y; double width = rect.width; double height = rect.height;
                String shape = predictShape(vertices,width,height,area,perimeter);


                //draw bounding box and description around object
                Imgproc.rectangle(frame, new Point(x,y),new Point(x+width, y+height),boundingBoxColor,2);
                Imgproc.putText(frame,("Area: "+ String.valueOf(area)), new Point(x+width+20,y+20),
                        3,0.7,boundingBoxColor,2);
                Imgproc.putText(frame,("Vertices: "+ String.valueOf(vertices)), new Point(x+width+20,y+45),
                        3,0.7,boundingBoxColor,2);
                Imgproc.putText(frame,("Shape: "+ shape), new Point(x+width+20,y+75),
                        3,0.7,boundingBoxColor,2);


                Log.i(CONTOURTAG, ("Area of contour: "+ String.valueOf(area)));
                Log.i(CONTOURTAG, ("Perimeter of contour: "+String.valueOf(perimeter)));
                Log.i(CONTOURTAG, ("Number of vertices: "+String.valueOf(vertices)));
                Log.i(CONTOURTAG, ("Shape: "+ shape));


                //draw contour
                List<MatOfPoint> singleContour = new ArrayList<>();
                singleContour.add(contours.get(i));
                Imgproc.drawContours(frame, singleContour,-1, contourColor,3);
            }

        }



    }
    public String predictShape(int vertices, double width,
                               double height, double area, double perimeter){
        double aspectRatio = width/ height;

        switch (vertices){
            case 4: {
                if (aspectRatio >= 0.95) {
                    return "Rectangle";
                } else return "Square";
            }
            case 3:
                return "Triangle";
            default: {
                if (vertices >= 7 && circularityMeasure(area, perimeter)) {
                    return "Circle";
                }
                else return "Others";
            }
        }
    }
    public boolean circularityMeasure(double area, double perimeter){
        if (perimeter == 0 || area == 0){
            return false;
        }
        double measure = (4*Math.PI*area)/(perimeter*perimeter);
        return measure >= 0.8;
    }
}