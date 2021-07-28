package com.example.ARCoreWithOpenCV;

import android.app.Application;
import android.content.Context;

//provides static access to context from anywhere in the app
public class MyApplication extends Application {
    private static Context context;

    public void onCreate() {
        super.onCreate();
        MyApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return MyApplication.context;
    }

}
