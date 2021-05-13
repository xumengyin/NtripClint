package com.xu.ntripclint.utils;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;

import androidx.annotation.RequiresApi;

import com.xu.ntripclint.AlarmService;
import com.xu.ntripclint.DamenService;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class Utils {

    //118.809563,32.023878
    private static final Double ManualLat = 32.023878;
    private static final Double ManualLon = 118.809563;

    public static String GenerateGGAFromLatLon() {
        String gga = "GPGGA,000001,";

        double posnum = Math.abs(ManualLat);
        double latmins = posnum % 1;
        int ggahours = (int) (posnum - latmins);
        latmins = latmins * 60;
        double latfracmins = latmins % 1;
        int ggamins = (int) (latmins - latfracmins);
        int ggafracmins = (int) (latfracmins * 10000);
        ggahours = ggahours * 100 + ggamins;
        if (ggahours < 1000) {
            gga += "0";
            if (ggahours < 100) {
                gga += "0";
            }
        }
        gga += ggahours + ".";
        if (ggafracmins < 1000) {
            gga += "0";
            if (ggafracmins < 100) {
                gga += "0";
                if (ggafracmins < 10) {
                    gga += "0";
                }
            }
        }
        gga += ggafracmins;
        if (ManualLat > 0) {
            gga += ",N,";
        } else {
            gga += ",S,";
        }

        posnum = Math.abs(ManualLon);
        latmins = posnum % 1;
        ggahours = (int) (posnum - latmins);
        latmins = latmins * 60;
        latfracmins = latmins % 1;
        ggamins = (int) (latmins - latfracmins);
        ggafracmins = (int) (latfracmins * 10000);
        ggahours = ggahours * 100 + ggamins;
        if (ggahours < 10000) {
            gga += "0";
            if (ggahours < 1000) {
                gga += "0";
                if (ggahours < 100) {
                    gga += "0";
                }
            }
        }
        gga += ggahours + ".";
        if (ggafracmins < 1000) {
            gga += "0";
            if (ggafracmins < 100) {
                gga += "0";
                if (ggafracmins < 10) {
                    gga += "0";
                }
            }
        }
        gga += ggafracmins;
        if (ManualLon > 0) {
            gga += ",E,";
        } else {
            gga += ",W,";
        }

        gga += "1,8,1,0,M,-32,M,3,0";

        String checksum = CalculateChecksum(gga);

        //Log.i("Manual GGA", "$" + gga + "*" + checksum);
        return "$" + gga + "*" + checksum;
    }

    public static String CalculateChecksum(String line) {
        int chk = 0;
        for (int i = 0; i < line.length(); i++) {
            chk ^= line.charAt(i);
        }
        String chk_s = Integer.toHexString(chk).toUpperCase(); // convert the integer to a HexString in upper case
        while (chk_s.length() < 2) { // checksum must be 2 characters. if it falls short, add a zero before the checksum
            chk_s = "0" + chk_s;
        }
        return chk_s;
    }

    public static void jobSchedule(Context context) {
        JobScheduler mJobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        ComponentName componentName = new ComponentName(context, DamenService.class);
        JobInfo.Builder builder = new JobInfo.Builder(99, componentName);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setMinimumLatency(20 * 1000);
            builder.setOverrideDeadline(25 * 1000);
        } else {
            builder.setPeriodic(10 * 1000);
        }
        mJobScheduler.schedule(builder.build());
    }


    //是否在白名单
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        boolean isIgnoring = false;
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (powerManager != null) {
            isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return isIgnoring;
    }

    //暂时不要
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void requestIgnoreBatteryOptimizations(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void getWake(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        wakeLock.acquire();
    }

    public static void alarmSchedule(Context context) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmService.class);
        PendingIntent pendingIntent =PendingIntent.getService(context,100,intent,FLAG_UPDATE_CURRENT);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 10*1000,20*1000,pendingIntent);
//        alarmManager.setAndAllowWhileIdle();
    }
    public static boolean isServiceRunning(Class<?> serviceClass,Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo runningService : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(runningService.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
