package com.xu.ntripclint;

import android.app.Application;

import com.xu.ntripclint.utils.CrashHandler;

public class Myapp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

//        IntentFilter filter =new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
//        registerReceiver(new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//
//            }
//        },);

        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);
    }
}
