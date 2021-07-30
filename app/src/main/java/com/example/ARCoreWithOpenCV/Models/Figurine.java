package com.example.ARCoreWithOpenCV.Models;

//base class of all figurines
public class Figurine {

    //name of figurine
    private String name;

    //shape of figurine
    private String shape;

    //wheather figurine has been detected in the current session
    private static boolean isDetected;

    public Figurine(String name,String shape) {
        this.name = name;
        this.shape = shape;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public static boolean isDetected() {
        return isDetected;
    }

    public static void setDetected(boolean detected) {
        isDetected = detected;
    }
}

