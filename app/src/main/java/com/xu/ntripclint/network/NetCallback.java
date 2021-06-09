package com.xu.ntripclint.network;

public abstract class NetCallback {

    protected void onReceive(byte[] allData) {

    }

    protected void onConnected() {

    }

    protected void ondisConnect() {

    }
    protected void onUploadNetError(String error,Throwable throwable) {

    }


}
