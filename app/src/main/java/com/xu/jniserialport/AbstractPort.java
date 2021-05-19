package com.xu.jniserialport;

import java.io.IOException;

/**
 * Created by W.
 */

public abstract class AbstractPort {

    OBDCallBack callback;
    boolean debugTest=false;
    public void setCallBack(OBDCallBack callBack) {
        this.callback = callBack;
    }

    public abstract void open() throws IOException;

    public abstract void close();

    public abstract void sendDataPackage(byte[] data) throws IOException;

    public abstract boolean isConnected();

    public void setDebugconnectTest(){
        debugTest=true;
    }

}
