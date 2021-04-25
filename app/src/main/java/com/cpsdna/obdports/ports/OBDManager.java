/**
 *
 */
package com.cpsdna.obdports.ports;


import android.content.Context;

import java.io.IOException;

/**
 * @author w
 */
public class OBDManager {

    private static OBDManager ourInstance;
    static Context mContext;
    AbstractPort abstractPort;

    public static OBDManager getInstance(Context context) {
        if (ourInstance == null) {
            ourInstance = new OBDManager(context);
        }
        return ourInstance;

    }

    private OBDManager(Context context) {
        mContext = context;
        abstractPort = new SerialPortImpl(mContext);
    }

    public void setCallBack(OBDCallBack callBack) {
        abstractPort.setCallBack(callBack);
    }

    public void setDebugConnectText() {
        abstractPort.setDebugconnectTest();
    }


    public void open() throws IOException {
        abstractPort.open();
    }

    public void close() {
        abstractPort.close();
    }

    public boolean isConnected() {
        return abstractPort.isConnected();
    }

    public void sendDataPackage(byte[] data) throws IOException {
        abstractPort.sendDataPackage(data);
    }

}
