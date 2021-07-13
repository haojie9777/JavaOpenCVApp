package com.example.ARCoreWithOpenCV;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.ARCoreWithOpenCV.common.helpers.CameraPermissionHelper;
import com.google.ar.core.ArCoreApk;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "MainActivity";
    private static String CONTOURTAG = "ContourSize";
    private Mat animeImage;
    private Mat maskImage;
    private CameraBridgeViewBase mOpenCvCameraView;

    //set to true to view imageview for images. Also need to uncomment in view
    private boolean debugMode = false;




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

        //Useful when we don't want to view openCV output and just want to view arcore
        //mOpenCvCameraView.setVisibility(SurfaceView.INVISIBLE);

         mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

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
        Log.i(TAG,"Called onCameraViewStarted()");
        //load image to animate with
        try {
            animeImage = Utils.loadResource(this, R.drawable.animeface, CvType.CV_8UC4); //with alpha channel
            maskImage = getPngMask(animeImage);

            //do some processing for png image background to be black when alpha =0
            double[] emptyPixel = new double[]{0.0, 0.0, 0.0, 0.0};
            for (int row = 0; row < animeImage.rows(); row++) {
                for (int col = 0; col < animeImage.cols(); col++) {
                    //check alpha value
                    double[] pixel = animeImage.get(row, col);
                    if (pixel[3] == 0) {
                        animeImage.put(row, col, emptyPixel);
                    }
                }
            }


            Imgproc.cvtColor(animeImage,animeImage,Imgproc.COLOR_BGRA2RGB); //remove alpha channel and convert to rgb
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onCameraViewStopped() {
        Log.i(TAG,"Called onCameraViewStopped()");
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.i(TAG,"Called onCameraFrame()");


        //Get a unmodified rgb copy of the original frame
        Mat originalFrame = inputFrame.rgba().clone();
        Imgproc.cvtColor(originalFrame, originalFrame,Imgproc.COLOR_RGBA2RGB);

        //apply gaussian blur on frame, then gray it
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
        List<MatOfPoint> contours = getContours(blurFrame);
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

        //only draw first contour
        boolean hasDrawn = false;

        for (int i = 0; i < contours.size(); i++){
            double area = Imgproc.contourArea(contours.get(i));

            //only process contours that are significant enough
            if (area >10000 && !hasDrawn) {

                //get list of points of contour
                MatOfPoint2f contour = new MatOfPoint2f(contours.get(i).toArray());
                //get perimeter of contour
                double perimeter = Imgproc.arcLength(contour,true);
                //find out polygon that best matches the contour
                MatOfPoint2f approxCurve = new MatOfPoint2f();
                Imgproc.approxPolyDP(contour,approxCurve,0.02*perimeter,true);
                //get number of vertices of the shape
                int vertices = approxCurve.toArray().length;

                //get starting coordinates (x,y), width and height of contour
                Rect rect = Imgproc.boundingRect(contours.get(i));
                double x = rect.x; double y = rect.y; double width = rect.width; double height = rect.height;

                //predict shape of object given its contour
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
                hasDrawn = true;

                //animate onto center of object containing the contour
                animateObject(frame,new Point(x +(width/4),y +(height/4)),width/2,height/2);
            }

        }



    }
    //predict type of shape
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

    //predict whether shape is circle or not
    public boolean circularityMeasure(double area, double perimeter){
        if (perimeter == 0 || area == 0){
            return false;
        }
        double measure = (4*Math.PI*area)/(perimeter*perimeter);
        return measure >= 0.8;
    }

    //animate anime features onto object detected
    public void animateObject(Mat frame, Point origin, double width, double height){

        //resize image to size matching contour's bounding box
        Mat animeImageResized = new Mat();
        Imgproc.resize(animeImage, animeImageResized,new Size(width,height));

        //Get frame without area that will be occupied by anime features
        Rect roiCrop = new Rect((int) origin.x,(int) origin.y,(int) width,(int) height);
        Mat roiArea = new Mat(frame, roiCrop);

        //retrieve mask and resize it
        Mat resizedMask = new Mat();
        Imgproc.resize(maskImage, resizedMask, new Size(width,height));

        //mask away anime features region
        Mat animeAreaMasked = roiArea.clone();
        Imgproc.cvtColor(animeAreaMasked,animeAreaMasked,Imgproc.COLOR_BGR2RGBA);
        Core.bitwise_and(roiArea,roiArea,animeAreaMasked,resizedMask);
        Imgproc.cvtColor(animeAreaMasked,animeAreaMasked,Imgproc.COLOR_RGBA2RGB);


        //add anime features to roi region
        Mat finalRoi = new Mat();
        Core.add(animeAreaMasked,animeImageResized,finalRoi); //1st one is ?? 2nd one is rgb
        Log.i("debug","animeAreaMasked= "+
                animeAreaMasked.channels() +" "+ animeAreaMasked.dims() +" "+ animeAreaMasked.size());
        Log.i("debug","animeImageResized= "+
                animeImageResized.channels() +" "+ animeImageResized.dims() +" "+ animeImageResized.size());


        //show selected images on imageview
        /*
        if (debugMode){
            Bitmap bmp = Bitmap.createBitmap(animeAreaMasked.cols(),animeAreaMasked.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(animeAreaMasked,bmp);

            Bitmap bmp2 = Bitmap.createBitmap(animeImage.cols(),animeImage.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(animeImage,bmp2);
            runOnUiThread(new Runnable() {
                public void run() {
                    ImageView imageView = (ImageView) findViewById(R.id.imageView);
                    ImageView imageView2 = (ImageView) findViewById(R.id.imageView2);

                    imageView.setImageBitmap(bmp);
                    imageView2.setImageBitmap(bmp2);
                }});
        }

         */

        //change roi region in original frame itself
        Mat subMat = frame.submat(new Rect((int) origin.x,(int) origin.y,(int) width,(int) height));
        finalRoi.copyTo(subMat);

    }

    //returns binary mask of a png image
    public Mat getPngMask(Mat image) {
        Mat mask = image.clone();
        double[] emptyPixel = new double[]{0.0, 0.0, 0.0, 0.0};
        double[] nonEmptyPixel = new double[]{255.0, 255.0, 255.0, 255.0};

        for (int row = 0; row < mask.rows(); row++) {
            for (int col = 0; col < mask.cols(); col++) {
                //check alpha value
                double[] pixel = mask.get(row, col);
                if (pixel[3] == 0) {
                    mask.put(row, col, emptyPixel);
                } else {
                    mask.put(row, col, nonEmptyPixel);
                }

            }
        }
        //remove alpha channel
        Imgproc.cvtColor(mask, mask, Imgproc.COLOR_BGRA2BGR);
        //get grayscale image
        Imgproc.cvtColor(mask, mask, Imgproc.COLOR_RGB2GRAY);
        //threshold image to get a mask
        Imgproc.threshold(mask, mask, 0, 255, Imgproc.THRESH_BINARY_INV);

        return mask;
    }


    }

