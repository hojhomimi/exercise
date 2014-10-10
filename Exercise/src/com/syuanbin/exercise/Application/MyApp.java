package com.syuanbin.exercise.Application;

import android.app.Application;

import com.syuanbin.exercise.data.RequestManager;


/**
 * Created by Administrator on 2014/10/10.
 */
public class MyApp extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }


    private void init() {
        RequestManager.init(this);
    }

}
