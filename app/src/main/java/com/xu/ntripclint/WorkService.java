package com.xu.ntripclint;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.cpsdna.obdports.ports.OBDCallBack;
import com.cpsdna.obdports.ports.OBDManager;
import com.cpsdna.obdports.ports.UtilityTools;
import com.xu.ntripclint.network.NetCallback;
import com.xu.ntripclint.network.NetManager;
import com.xu.ntripclint.ntrip.NtripCallBack;
import com.xu.ntripclint.ntrip.NtripManager;
import com.xu.ntripclint.pojo.ConfigBean;
import com.xu.ntripclint.utils.LocManager;
import com.xu.ntripclint.utils.Logs;
import com.xu.ntripclint.utils.Utils;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 */
public class WorkService extends Service {
    /**
     * 模拟gpgga 发送给ntrip
     */
    public static final boolean mockGpgga = false;
    public boolean mockUpload = false;
    //复用ntrip通道上传
    public boolean reUseNtripChanel = false;
    String currentFixNmea;
    String currentNmea;
    private final WorkBinder workBinder = new WorkBinder();
    private NetManager netManager;
    private NtripManager ntripManager = NtripManager.getInstance();
    private OBDManager obdManager;
    private final LocManager.LocChangeLisener locChangeLisener = new LocManager.LocChangeLisener() {
        @Override
        public void onLocationChanged(Location amapLocation) {

            Logs.w("location:" + amapLocation.getLatitude() + "::" + amapLocation.getLongitude());
            //回调
//            Logs.d();
        }
    };
    private final LocManager.LocChangeNmeaLisener nemaLisener = new LocManager.LocChangeNmeaLisener() {
        @Override
        public void onLocationChanged(String nmea, long time) {
            if (nmea.contains("GPGGA")) {
                String[] result = nmea.split(",");
                if (result.length >= 11) {
                    try {
                        if (!TextUtils.isEmpty(result[2]) && !TextUtils.isEmpty(result[4])) {
                            double lat = Double.parseDouble(result[2].substring(0, 2)) + (Double.parseDouble(result[2].substring(2)) / 60);
                            double lng = Double.parseDouble(result[4].substring(0, 3)) + (Double.parseDouble(result[4].substring(3)) / 60);
                            // Logs.w("解析Gpgga经纬度:" + lat + "::" + lng);
                        }
                        int status = Integer.parseInt(result[6]);
                        //Logs.w("解析Gpgga-----" +nmea+"---"+ GetnSolutionState(status));
                        //是差分解
                        if (status == 2) {
                            currentFixNmea = nmea;
                        }
                        currentNmea = nmea;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    };
    IServiceCallBack serviceCallBack;

    public void setServiceCallBack(IServiceCallBack callBack) {
        serviceCallBack = callBack;
    }

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

        public WorkService getService() {
            return WorkService.this;
        }
    }

    public void switchUploadGpgga(boolean upload) {
        if (upload) {
            startNtripTimer(2000);
        } else {
            cancleNtripTimer();
        }

    }

    ConfigBean ntripconfigData, uploadConfig;

    /**
     * 设置配置参数
     *
     * @param bean
     */
    public void setNtipConfigData(ConfigBean bean, boolean uploadGga) {
        ntripconfigData = bean;
        ntripManager.setServer(bean);
        ntripManager.setCallBack(new NtripCallBack() {
            @Override
            public void onReceive(byte[] data) {
                //主线程 发给串口 todo

                Logs.w("ntrp接收信息" + UtilityTools.bytesToHexString(data));
                try {
                    obdManager.sendDataPackage(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConnected() {
                serviceCallBack.onNtripStatus(IServiceCallBack.STATUS_OK, null);
                if (uploadGga)
                    startNtripTimer(2000);
            }

            @Override
            public void onDisConnect(String error) {
                serviceCallBack.onNtripStatus(IServiceCallBack.STATUS_ERROR, error);
            }

            @Override
            public void onReceiveDebug(String error) {
                serviceCallBack.ntripDebugData(error);
            }
        });
        ntripManager.connectServer();
    }

    /**
     * 开启上报
     *
     * @param bean
     */
    public void setUploadConfigData(ConfigBean bean) {
        reUseNtripChanel=false;
        uploadConfig = bean;
        netManager.setConfig(bean.uploadServer, bean.uploadPort);
        netManager.start(new NetCallback() {
            @Override
            protected void onConnected() {
                serviceCallBack.onNetStatus(IServiceCallBack.STATUS_OK);
            }

            @Override
            protected void ondisConnect() {
                serviceCallBack.onNetStatus(IServiceCallBack.STATUS_ERROR);
            }

            @Override
            protected void onReceive(byte[] allData) {
                super.onReceive(allData);
                Logs.d("net data:" + UtilityTools.bytesToHexString(allData));
            }
        });
        StartUploadData(uploadConfig.uploadTime * 1000);
    }

    public void setReUseNtripChanel(boolean reUseNtripChanel) {
        this.reUseNtripChanel = reUseNtripChanel;
    }

    public void reuseNtripChanel(ConfigBean bean) {
        reUseNtripChanel = true;
        uploadConfig=bean;
        serviceCallBack.onNetStatus(IServiceCallBack.STATUS_OK);
        StartUploadData(uploadConfig.uploadTime * 1000);
    }

    Timer upLoadTimer, sendNtripTimer;
    TimerTask upLoadTimerTask, sendNtripTask;

    public void setMockUpload(boolean mockUpload) {
        this.mockUpload = mockUpload;
    }

    public void StartUploadData(long frequence) {
        cancleUploadTimer();
        upLoadTimer = new Timer();
        upLoadTimerTask = new TimerTask() {
            @Override
            public void run() {
                String data;
                if (mockUpload) {
                    data = Utils.GenerateGGAFromLatLon();
                } else {
                    data = currentFixNmea;
                }
                if (!TextUtils.isEmpty(data)) {
                    try {
                        if (!reUseNtripChanel)
                            netManager.writeDirect(data, getBattery());
                        else
                            ntripManager.writeUploadData(data, getBattery());
                    } catch (IOException e) {
                        Logs.w("写入net数据异常");
                        e.printStackTrace();
                    }
                }
            }
        };
        upLoadTimer.scheduleAtFixedRate(upLoadTimerTask, 1000, frequence);
    }

    public void startNtripTimer(long frequence) {
        cancleNtripTimer();
        sendNtripTimer = new Timer();
        sendNtripTask = new TimerTask() {
            @Override
            public void run() {
                //todo 测试模拟数据
                if (mockGpgga) {
                    ntripManager.sendGPGGA(Utils.GenerateGGAFromLatLon());
                    return;
                }
                if (!TextUtils.isEmpty(currentNmea)) {
                    ntripManager.sendGPGGA(currentNmea);
                }
            }
        };
        sendNtripTimer.scheduleAtFixedRate(sendNtripTask, 1000, frequence);
    }

    private int getBattery() {
        BatteryManager manager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int battery = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        Logs.d("电池:" + battery);
        return battery;
    }

    private void cancleUploadTimer() {
        if (upLoadTimerTask != null) {
            upLoadTimerTask.cancel();
            upLoadTimerTask = null;
        }
        if (upLoadTimer != null) {
            upLoadTimer.cancel();
            upLoadTimer = null;
        }
    }

    private void cancleNtripTimer() {
        if (sendNtripTask != null) {
            sendNtripTask.cancel();
            sendNtripTask = null;
        }
        if (sendNtripTimer != null) {
            sendNtripTimer.cancel();
            sendNtripTimer = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return workBinder;
    }

    Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(110, new Notification());
        }
        obdManager = OBDManager.getInstance(this);
        netManager = NetManager.getInstance(this);
        LocManager locManager = LocManager.getInstance(getApplicationContext());
        locManager.addListener(locChangeLisener);
        locManager.addListener(nemaLisener);
        locManager.openLocation(2000, 0);
        obdManager.setCallBack(new OBDCallBack() {
            @Override
            protected void onReceive(byte[] allData) {
                Logs.e("接收串口数据" + UtilityTools.bytesToHexString(allData));
            }

            @Override
            protected void onStart() {
                super.onStart();
                if (serviceCallBack != null)
                    serviceCallBack.onSerialPortStatus(IServiceCallBack.STATUS_OK);

            }

            @Override
            protected void onError(Exception e) {
                if (serviceCallBack != null)
                    serviceCallBack.onSerialPortStatus(IServiceCallBack.STATUS_ERROR);
            }
        });
        //演示开启串口
        mainHandler.postDelayed((Runnable) () -> {
            try {
                obdManager.open();
            } catch (IOException e) {
                e.printStackTrace();
                Logs.e("串口失败啦");
            }
        }, 2000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancleUploadTimer();
        cancleNtripTimer();
        LocManager.getInstance(getApplicationContext()).removeListener(locChangeLisener);
        LocManager.getInstance(getApplicationContext()).removeListener(nemaLisener);
        obdManager.close();
        ntripManager.disconnectServer();
        netManager.close();

    }
}
