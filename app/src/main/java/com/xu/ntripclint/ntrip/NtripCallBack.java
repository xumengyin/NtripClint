package com.xu.ntripclint.ntrip;

public interface NtripCallBack {
    void onReceive(byte[]data);
    void onConnected();
    void onDisConnect(String error);
}
