package com.example.ARCoreWithOpenCV.Models;

//base class of all figurines
public class Figurine {

    //name of figurine
    private String name;

    //shape of figurine
    private String shape;

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
}

