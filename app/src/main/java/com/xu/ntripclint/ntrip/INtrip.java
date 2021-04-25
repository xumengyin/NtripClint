package com.xu.ntripclint.ntrip;

public interface INtrip {

    void connectServer();

    void setServer(String server,String port,String mountPoint,String name,String pass);

    void disconnectServer();
}
