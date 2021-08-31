package com.jerry.ntripwatchdog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WatchDogReceiver extends BroadcastReceiver {
    private static final String TAG = "WatchDogReceiver";
    public static final String WatchDogFilter="com.xu.WatchDogReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "WatchDogReceiver onReceive: ");
        context.startService(new Intent(context,WatchService.class));
    }
}
