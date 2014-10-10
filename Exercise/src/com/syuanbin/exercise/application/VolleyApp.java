/*
 * Created by Storm Zhang, Feb 11, 2014.
 */

package com.syuanbin.exercise.application;

import android.app.Application;

import com.syuanbin.exercise.data.RequestManager;

public class VolleyApp extends Application {
	@Override
    public void onCreate() {
        super.onCreate();
        init();
    }


    private void init() {
        RequestManager.init(this);
    }
}
