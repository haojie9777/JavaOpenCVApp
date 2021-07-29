package com.example.ARCoreWithOpenCV;

import android.app.Application;
import android.content.Context;

//provides static access to context from anywhere in the app
public class MyApplication extends Application {
    private static Context context;
    private static int viewMode = 0;

    public void onCreate() {
        super.onCreate();
        MyApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return MyApplication.context;
    }

    public static int getViewMode() {
        return viewMode;
    }

    public static void toggleViewMode() {
        if (viewMode == 0) viewMode =1;
        else viewMode = 0;

    }



}
