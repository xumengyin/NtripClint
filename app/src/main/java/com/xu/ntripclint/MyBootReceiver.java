package com.xu.ntripclint;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.xu.ntripclint.activity.SplashActivity;
import com.xu.ntripclint.utils.Logs;
import com.xu.ntripclint.utils.Utils;

/**
 * 自启动广播
 */
public class MyBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //
        //Logs.d("BootReceiver启动了");
        Logs.w("BootReceiver启动了" + intent.getAction());
        Logs.w("BootReceiver启动了" + intent.getAction());
        Logs.w("BootReceiver启动了" + intent.getAction());
        Log.d("xuxu", "MyBootReceiver: fffffffffffffffffffffff");
//        Intent intent1 =new Intent(context,MainActivity.class);
//        intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(intent1);

        logic2(context);
        Utils.alarmSchedule(context);
    }

    private void logic(Context context) {
        Intent intent = new Intent(context, WorkService.class);
        intent.putExtra(WorkService.START_TAG, true);
        context.startService(intent);
    }

    private void logic2(Context context) {
        Intent intent = new Intent(context, SplashActivity.class);
        intent.putExtra(WorkService.SELF_START, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

}
