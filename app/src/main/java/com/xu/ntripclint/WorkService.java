package com.xu.ntripclint;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.xu.ntripclint.utils.LocManager;
import com.xu.ntripclint.utils.Logs;

/**
 *
 */
public class WorkService extends Service {


    private final WorkBinder workBinder = new WorkBinder();

    private final LocManager.LocChangeLisener locChangeLisener=new LocManager.LocChangeLisener() {
        @Override
        public void onLocationChanged(Location amapLocation) {

            Logs.w("location:"+amapLocation.getLatitude()+"::"+amapLocation.getLongitude());
                //回调
//            Logs.d();
        }
    };
    private final LocManager.LocChangeNmeaLisener nemaLisener= (nmea, time) -> {
        if(nmea.contains("GPGGA")){
            String[] result = nmea.split(",");
            if(result.length >= 11){
                try {
                    if(!TextUtils.isEmpty(result[2])&&!TextUtils.isEmpty(result[4]))
                    {
                        double lat =Double.parseDouble(result[2].substring(0,2))+(Double.parseDouble(result[2].substring(2))/60);
                        double lng =Double.parseDouble(result[4].substring(0,3))+(Double.parseDouble(result[4].substring(3))/60);
                        Logs.w("解析Gpgga经纬度:"+lat+"::"+lng);
                    }
                    Logs.w("解析Gpgga-----"+GetnSolutionState(Integer.parseInt(result[6])));
                }catch (Exception e)
                {
                    e.printStackTrace();
                }

            }
        }

    };

    private String GetnSolutionState(int nType) {
        String strSolutionState = "";
        switch (nType) {
            case 0:
                strSolutionState = "无效解";
                break;
            case 1:
                strSolutionState = "单点解"; //非差分定位
                break;
            case 2:
                strSolutionState = "差分解";
                break;
            case 3:
                strSolutionState = "无效PPS";
                break;
            case 4:
                strSolutionState = "固定解";
                break;
            case 5:
                strSolutionState = "浮点解";
                break;
            case 6:
                strSolutionState = "正在估算";
                break;
            default:
                strSolutionState = "" + nType;
                break;
        }
        return strSolutionState;
    }


    public class WorkBinder extends Binder {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return workBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LocManager locManager = LocManager.getInstance(getApplicationContext());
        locManager.addListener(locChangeLisener);
        locManager.addListener(nemaLisener);
        locManager.openLocation(2000, 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocManager.getInstance(getApplicationContext()).removeListener(locChangeLisener);
        LocManager.getInstance(getApplicationContext()).removeListener(nemaLisener);
    }
}
