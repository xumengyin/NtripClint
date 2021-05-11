package com.xu.ntripclint;

import android.app.ActivityManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;

import com.xu.ntripclint.utils.FileLogUtils;
import com.xu.ntripclint.utils.Logs;

public class DamenService extends JobService {
    @Override
    public void onCreate() {
        super.onCreate();
        Logs.d("DamenService onCreate---------");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logs.d("DamenService onDestroy---------");
    }

    @Override
    public boolean onStartJob(JobParameters params) {

        Logs.d("DamenService onStartJob---------");
        FileLogUtils.writeLogtoFile("DamenService onStartJob");
        if(!isServiceRunning(WorkService.class))
        {
            FileLogUtils.writeLogtoFile("DamenService ServiceNNNNNNNRunning");
            Intent intent = new Intent(this, WorkService.class);
            intent.putExtra(WorkService.START_TAG, true);
            startService(intent);
        }else
        {
            FileLogUtils.writeLogtoFile("DamenService ServiceRunning");
        }
       // jobFinished(params, false);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }


    /**
     * 判断保活Service是否启动
     * @param serviceClass
     * @return
     */
    public boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo runningService : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(runningService.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
