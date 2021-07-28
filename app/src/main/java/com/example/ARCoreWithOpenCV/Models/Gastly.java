package com.example.ARCoreWithOpenCV.Models;

import android.content.Context;
import android.util.Log;

import com.example.ARCoreWithOpenCV.MyApplication;
import com.example.ARCoreWithOpenCV.R;
import com.example.ARCoreWithOpenCV.utils.ImageProcessing;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;

public class Gastly extends Figurine {
    private static Mat overlayImage = new Mat();
    private static Mat overlayMask = new Mat();
    private static final String TAG = "Gastly";

    public Gastly() throws IOException {
        super("Gastly","Circle");
        Context context = MyApplication.getAppContext();
        overlayImage = Utils.loadResource(context, R.drawable.gastly, CvType.CV_8UC4); //with alpha channel
        overlayMask = ImageProcessing.getPngMask(overlayImage);
        //remove alpha channel and make background black
        overlayImage = ImageProcessing.processOverlayImage(overlayImage);
    }

    public static Mat getOverlayImage() {
        return overlayImage;
    }

    public static void setOverlayImage(Mat overlayImage) {
        Gastly.overlayImage = overlayImage;
    }

    public static Mat getOverlayMask() {
        return overlayMask;
    }

    public static void setOverlayMask(Mat overlayMask) {
        Gastly.overlayMask = overlayMask;
    }





}
