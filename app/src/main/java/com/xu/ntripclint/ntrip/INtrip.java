package com.xu.ntripclint.ntrip;

import com.xu.ntripclint.pojo.ConfigBean;

public interface INtrip {

    void connectServer();

    void setServer(ConfigBean bean);

    void disconnectServer();

    void setCallBack(NtripCallBack callBack);

    void sendGPGGA(String gpgga);
}
