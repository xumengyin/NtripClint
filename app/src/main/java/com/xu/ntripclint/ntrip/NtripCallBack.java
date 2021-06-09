package com.xu.ntripclint.ntrip;

public interface NtripCallBack {
    void onReceive(byte[]data);
    void onConnected();
    void onDisConnect(String error);
    void onReceiveDebug(String error);
    void onReceiveNetError(String error,Throwable e);
}
