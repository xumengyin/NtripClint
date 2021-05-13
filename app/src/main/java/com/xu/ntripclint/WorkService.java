package com.xu.ntripclint;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
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
import com.xu.ntripclint.utils.FileLogUtils;
import com.xu.ntripclint.utils.LocManager;
import com.xu.ntripclint.utils.Logs;
import com.xu.ntripclint.utils.ScreenBroadcastListener;
import com.xu.ntripclint.utils.ScreenManager;
import com.xu.ntripclint.utils.Storage;
import com.xu.ntripclint.utils.Utils;

import java.io.IOException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 */
public class WorkService extends Service {
    /**
     * 开机启动的配置
     */
    public static final String START_TAG = "START_TAG";


    public static final String SELF_START = "SELF_START";
    /**
     * 模拟gpgga 发送给ntrip
     */
    public static final boolean mockGpgga = false;
    public boolean mockUpload = false;
    public boolean recorderLoc = false;
    //复用ntrip通道上传
    public boolean reUseNtripChanel = false;
    String currentFixNmea;
    String currentNmea;
    private final WorkBinder workBinder = new WorkBinder();

    HandlerThread recorderHandThread = new HandlerThread("recorderHandThread");
    Handler recorderHandler;

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
            Logs.w("leame data：" + nmea);
            if (nmea.contains("GNGGA")) {
                String[] result = nmea.split(",");
                if (result.length >= 11) {
                    try {
                        if (!TextUtils.isEmpty(result[2]) && !TextUtils.isEmpty(result[4])) {
                            double lat = Double.parseDouble(result[2].substring(0, 2)) + (Double.parseDouble(result[2].substring(2)) / 60);
                            double lng = Double.parseDouble(result[4].substring(0, 3)) + (Double.parseDouble(result[4].substring(3)) / 60);
                            // Logs.w("解析Gpgga经纬度:" + lat + "::" + lng);
                            int status = Integer.parseInt(result[6]);
                            String gpsStats = status + " " + GetnSolutionState(status);
                            Logs.w("解析GNGGA-----" + nmea + "---" + gpsStats);
                            //是差分解
                            //if (status == 2) {
                            currentFixNmea = nmea;
                            if (serviceCallBack != null)
                                serviceCallBack.onNmeaRecieve(nmea, lat, lng, gpsStats);

                            //}
                        }

                        currentNmea = nmea;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
            if (recorderLoc) {
                Message message = recorderHandler.obtainMessage();
                message.obj = nmea;
                recorderHandler.sendMessage(message);
            }
        }
    };
    IServiceCallBack serviceCallBack;

    public void setServiceCallBack(IServiceCallBack callBack) {
        serviceCallBack = callBack;
    }

    public void setRecorderLoc(boolean recorderLoc) {
        this.recorderLoc = recorderLoc;
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


    private void initRecorderHandler() {
        recorderHandThread.start();
        recorderHandler = new Handler(recorderHandThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                String obj = (String) msg.obj;
                FileLogUtils.writeLogtoFile(obj);
            }
        };
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
                if (serviceCallBack != null)
                    serviceCallBack.onNtripStatus(IServiceCallBack.STATUS_OK, null);
                if (uploadGga)
                    startNtripTimer(2000);
            }

            @Override
            public void onDisConnect(String error) {
                if (serviceCallBack != null)
                    serviceCallBack.onNtripStatus(IServiceCallBack.STATUS_ERROR, error);
            }

            @Override
            public void onReceiveDebug(String error) {
                if (serviceCallBack != null)
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
        reUseNtripChanel = false;
        uploadConfig = bean;
        netManager.setConfig(bean.uploadServer, bean.uploadPort);
        netManager.start(new NetCallback() {
            @Override
            protected void onConnected() {
                if (serviceCallBack != null)
                    serviceCallBack.onNetStatus(IServiceCallBack.STATUS_OK);
            }

            @Override
            protected void ondisConnect() {
                if (serviceCallBack != null)
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
        uploadConfig = bean;
        if (serviceCallBack != null)
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


    private void init1pxActivity()
    {
        final ScreenManager screenManager = ScreenManager.getInstance(this);
        ScreenBroadcastListener listener = new ScreenBroadcastListener(this);
        listener.registerListener(new ScreenBroadcastListener.ScreenStateListener() {
            @Override
            public void onScreenOn() {
                screenManager.finishActivity();
            }
            @Override
            public void onScreenOff() {
                screenManager.startActivity();
            }
        });
    }
    @Override
    public void onCreate() {
        super.onCreate();
        FileLogUtils.writeLogtoFile("service onCreate"+toString());
        Utils.jobSchedule(this);
       // Utils.getWake(this);
        initRecorderHandler();
       // init1pxActivity();
        obdManager = OBDManager.getInstance(this);
        netManager = NetManager.getInstance(this);
        LocManager locManager = LocManager.getInstance(getApplicationContext());
        locManager.addListener(locChangeLisener);
        locManager.addListener(nemaLisener);
        locManager.openLocation(0, 0);
        obdManager.setCallBack(new OBDCallBack() {
            @Override
            protected void onReceive(byte[] allData) {
                String str = new String(allData);
                Logs.e("接收串口数据" + str);
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
        if (intent != null) {
            boolean booleanExtra = intent.getBooleanExtra(START_TAG, false);
            if (booleanExtra) {
                startAll();
            }
        } else {
            startAll();
        }
        startForeground(101, new Notification());
        return Service.START_STICKY;
    }

    private void startAll() {
        ConfigBean configBean = Storage.getData(this);
        setNtipConfigData(configBean, false);
        if (Objects.equals(configBean.ntripServer, configBean.uploadServer) && configBean.uploadPort == configBean.ntripServerPort) {
            reuseNtripChanel(configBean);
        } else {
            setUploadConfigData(configBean);
        }
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
        recorderHandThread.quitSafely();
        FileLogUtils.writeLogtoFile("service onDestroy");
    }
}
