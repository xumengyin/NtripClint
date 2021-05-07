package com.xu.ntripclint;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.xu.ntripclint.utils.Logs;

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

        logic(context);
    }

    private void logic(Context context) {
        Intent intent = new Intent(context, WorkService.class);
        intent.putExtra(WorkService.START_TAG, true);
        context.startService(intent);
    }

}
