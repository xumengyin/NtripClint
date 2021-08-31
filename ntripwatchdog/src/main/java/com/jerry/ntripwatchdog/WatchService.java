package com.jerry.ntripwatchdog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

public class WatchService extends Service {

    private static final String TAG = "WatchService";
    long gap =30*1000;
    Timer timer;
    TimerTask task;
    public static final String watchDog="com.jerry.watchDogAction";
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initTimer()
    {
        destroyTimer();
        timer=new Timer();
        task=new TimerTask() {
            @Override
            public void run() {

                Log.d(TAG, "run: send wong wong wong!");
                Intent intent =new Intent(watchDog);
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                sendBroadcast(intent);
            }
        };
        timer.schedule(task,1000,gap);
    }
    private void destroyTimer()
    {
        if (timer!=null) {
            timer.cancel();
            timer=null;
        }
        if(task!=null)
        {
            task.cancel();
            task=null;
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "WatchService onStartCommand: ");
        if(timer==null||task==null)
        {
            initTimer();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyTimer();
        Log.d(TAG, "WatchService onDestroy: ");
    }
}
