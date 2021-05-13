package com.xu.ntripclint;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.xu.ntripclint.utils.FileLogUtils;
import com.xu.ntripclint.utils.Logs;
import com.xu.ntripclint.utils.Utils;


public class AlarmService extends IntentService {

    Handler handler =new Handler(Looper.getMainLooper());
    public AlarmService() {
        super("NtripAlarmService");
    }
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Logs.d("AlarmService  onHandleIntent");
        FileLogUtils.writeLogtoFile("AlarmService onHandleIntent");
        handler.post(() -> {
            if (!Utils.isServiceRunning(WorkService.class, AlarmService.this)) {
                Logs.d("AlarmService  isService not run");
                Intent intents = new Intent(AlarmService.this, WorkService.class);
                intents.putExtra(WorkService.START_TAG, true);
                startService(intent);

            }
        });

    }
}
