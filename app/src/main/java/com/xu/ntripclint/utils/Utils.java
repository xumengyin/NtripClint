package com.xu.ntripclint.utils;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.RequiresApi;

public class Utils {

    //118.809563,32.023878
    private static final Double ManualLat = 32.023878;
    private static final Double ManualLon = 118.809563;
    public static String GenerateGGAFromLatLon() {
        String gga = "GPGGA,000001,";

        double posnum = Math.abs(ManualLat);
        double latmins = posnum % 1;
        int ggahours = (int)(posnum - latmins);
        latmins = latmins * 60;
        double latfracmins = latmins % 1;
        int ggamins = (int)(latmins - latfracmins);
        int ggafracmins = (int)(latfracmins * 10000);
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
        ggahours = (int)(posnum - latmins);
        latmins = latmins * 60;
        latfracmins = latmins % 1;
        ggamins = (int)(latmins - latfracmins);
        ggafracmins = (int)(latfracmins * 10000);
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
//    @RequiresApi(api = Build.VERSION_CODES.M)
//    public void requestIgnoreBatteryOptimizations() {
//        try {
//            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
//            intent.setData(Uri.parse("package:" + getPackageName()));
//            startActivity(intent);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
