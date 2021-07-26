package com.example.ARCoreWithOpenCV.utils;

import android.content.Context;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
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

public class ImageProcessing {

    private static Mat overlayImage;
    private static Mat overlayMask;

    private void initStaticImages(){
        overlayImage = new Mat();
        overlayMask = new Mat();
    }

    public ImageProcessing(Context context){
        BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(context) {
            @Override
            public void onManagerConnected(int status) {
            }
        };
        if (!OpenCVLoader.initDebug()) {
            //Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, context, mLoaderCallback);
        } else {
            //Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        initStaticImages();
    }


    public static void setOverlayImage(Mat image){
        overlayImage = image.clone();

        //do some processing for png image background to be black when alpha =0, else will have issues with non-uniform png images
        double[] emptyPixel = new double[]{0.0, 0.0, 0.0, 0.0};
        for (int row = 0; row < overlayImage.rows(); row++) {
            for (int col = 0; col < overlayImage.cols(); col++) {
                //check alpha value
                double[] pixel = overlayImage.get(row, col);
                if (pixel[3] == 0) {
                    overlayImage.put(row, col, emptyPixel);
                }
            }
        }
        Imgproc.cvtColor(overlayImage,overlayImage,Imgproc.COLOR_BGRA2RGB); //remove alpha channel and convert to rgb
    }

    public void setMaskImage(Mat image){
        overlayMask = image.clone();
    }

    //Draw the first contour and also overlay a png onto it
    public void drawContours(List<MatOfPoint> contours, Mat frame,Point lastTouchedCoordinates) {
        Scalar contourColor = new Scalar(0, 255, 0);
        Scalar boundingBoxColor = new Scalar(255, 255, 0);

        //only draw first contour
        boolean hasDrawn = false;

        for (int i = 0; i < contours.size(); i++) {
            double area = Imgproc.contourArea(contours.get(i));

            //only process contours that are significant enough
            if (area > 10000 && !hasDrawn) {

                //get list of points of contour
                MatOfPoint2f contour = new MatOfPoint2f(contours.get(i).toArray());
                //get perimeter of contour
                double perimeter = Imgproc.arcLength(contour, true);
                //find out polygon that best matches the contour
                MatOfPoint2f approxCurve = new MatOfPoint2f();
                Imgproc.approxPolyDP(contour, approxCurve, 0.02 * perimeter, true);
                //get number of vertices of the shape
                int vertices = approxCurve.toArray().length;

                //get starting coordinates (x,y), width and height of contour
                Rect rect = Imgproc.boundingRect(contours.get(i));

                if (!rect.contains(lastTouchedCoordinates)) return;
                double x = rect.x;
                double y = rect.y;
                double width = rect.width;
                double height = rect.height;

                Log.i("MainActivity", Double.toString(x + (width/2)));

                //predict shape of object given its contour
                String shape = predictShape(vertices, width, height, area, perimeter);
                //only proceed if is circle for now


                //draw bounding box and description around object
                Imgproc.rectangle(frame, new Point(x, y), new Point(x + width, y + height), boundingBoxColor, 2);
                Imgproc.putText(frame, ("Area: " + area), new Point(x + width + 20, y + 20),
                        3, 0.7, boundingBoxColor, 2);
                Imgproc.putText(frame, ("Vertices: " + vertices), new Point(x + width + 20, y + 45),
                        3, 0.7, boundingBoxColor, 2);
                Imgproc.putText(frame, ("Shape: " + shape), new Point(x + width + 20, y + 75),
                        3, 0.7, boundingBoxColor, 2);


                //draw contour
                List<MatOfPoint> singleContour = new ArrayList<>();
                singleContour.add(contours.get(i));
                Imgproc.drawContours(frame, singleContour, -1, contourColor, 3);
                hasDrawn = true;

                //apply overlay onto center of object containing the contour
                applyOverlay(frame, new Point(x + (width / 4), y + (height / 4)), width / 2, height / 2);
            }

        }
    }

    //Get list of all contours in the frame
    public List<MatOfPoint> getContours(Mat frame){
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(frame,contours, hierarchy, Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_NONE);
        return contours;
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
        return measure >= 0.70;
    }

    //apply overlay onto given points in the frame
    public void applyOverlay(Mat frame, Point origin, double width, double height){
        //resize image to size matching contour's bounding box
        Mat animeImageResized = new Mat();
        Imgproc.resize(overlayImage, animeImageResized,new Size(width,height));

        //Get frame without area that will be occupied by anime features
        Rect roiCrop = new Rect((int) origin.x,(int) origin.y,(int) width,(int) height);
        Mat roiArea = new Mat(frame, roiCrop);

        //retrieve mask and resize it
        Mat resizedMask = new Mat();
        Imgproc.resize(overlayMask, resizedMask, new Size(width,height));

        //mask away anime features region
        Mat animeAreaMasked = roiArea.clone();
        Imgproc.cvtColor(animeAreaMasked,animeAreaMasked,Imgproc.COLOR_BGR2RGBA);
        Core.bitwise_and(roiArea,roiArea,animeAreaMasked,resizedMask);
        Imgproc.cvtColor(animeAreaMasked,animeAreaMasked,Imgproc.COLOR_RGBA2RGB);

        //add anime features to roi region
        Mat finalRoi = new Mat();
        Core.add(animeAreaMasked,animeImageResized,finalRoi);

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
