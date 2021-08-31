package com.xu.ntripclint;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.xu.ntripclint.activity.SplashActivity;
import com.xu.ntripclint.utils.Logs;
import com.xu.ntripclint.utils.Utils;

/**
 * 收另一个app的广播
 */
public class WorkWatchReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("xuxu", "WorkWatchReceiver: fffffffffffffffffffffff");
//        Intent intent1 =new Intent(context,MainActivity.class);
//        intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(intent1);
        if (!Utils.isServiceRunning(WorkService.class, context)) {
            Logs.d("WorkWatchReceiver  isService not run");
            Intent workInt = new Intent(context, WorkService.class);
            workInt.putExtra(WorkService.START_TAG, true);
            context.startService(workInt);

        }
    }

}
