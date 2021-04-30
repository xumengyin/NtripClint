package com.xu.ntripclint;

public interface IServiceCallBack {

    int STATUS_OK=0;
    int STATUS_ERROR=1;
    void onSerialPortStatus(int status);

    void onNetStatus(int status);

    void onNtripStatus(int status,String error);

    void ntripDebugData(String data);

    void onNmeaRecieve(String data,double lat,double lng,String gpsStatus);
}
