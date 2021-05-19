package com.xu.jniserialport;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.xu.ntripclint.utils.Logs;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by xu
 */

public class SerialPortImpl extends AbstractPort {


    public static final String TEST_DEVICE = "/dev/ttyS0";
    public static final int TEST_BAUDRATE = 115200;
//    public static final String TEST_DEVICE = "/dev/ttyMT0";
//    public static final int TEST_BAUDRATE = 19200;

    Context mContext;
    com.xu.jniserialport.DNASerialPort DNASerialPort;
    public OutputStream mOutputStream;
    InputStream mInputStream;
    ReadThread readThread;
    HandlerThread dealThread = new HandlerThread("SerialPortImpl");
    Handler dealHandler;

    public SerialPortImpl(Context context) {
        mContext = context;
        dealThread.start();
        initHandler();
    }

    private void initHandler() {
        dealHandler = new Handler(dealThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                //发送数据
                if (msg.what == 1) {
                    try {
                        sendData((byte[]) msg.obj);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    public void open() throws IOException {
        if (mInputStream != null) {
            close();
        }

        String device = TEST_DEVICE;
        int baudrate = TEST_BAUDRATE;
        try {
            DNASerialPort = new DNASerialPort(new File(device), baudrate, 0);
            mOutputStream = DNASerialPort.getOutputStream();
            mInputStream = DNASerialPort.getInputStream();
            if (mOutputStream != null && mInputStream != null) {
                if (callback != null) {
                    callback.onStart();
                }
                //不要读串口 gps信息可以从串口或者定位api获取，读了串口 定位api就返回信息很慢
//                readThread = new ReadThread();
//                readThread.start();
            }
        } catch (SecurityException e) {
            Logs.e("串口打开失败");
            e.printStackTrace();
        }
    }

    private void dealException() {
        try {
            Logs.d("dealException开始");
            if (mOutputStream != null) {
                mOutputStream.close();
                mOutputStream = null;
            }
            if (mInputStream != null) {
                mInputStream.close();
                mInputStream = null;
            }
            String device = TEST_DEVICE;
            int baudrate = TEST_BAUDRATE;
            DNASerialPort = new DNASerialPort(new File(device), baudrate, 0);
            mOutputStream = DNASerialPort.getOutputStream();
            mInputStream = DNASerialPort.getInputStream();
//            if (mOutputStream != null && mInputStream != null) {
//
//            }
        } catch (SecurityException e) {
            Logs.e("dealException串口打开失败SecurityException");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            Logs.e("dealException 串口打开失败 IOException");
            e.printStackTrace();
        }
    }

    public void close() {
        if (dealThread != null) {
            dealThread.quit();
        }
        if (readThread != null) {
            readThread.interrupt();
            readThread = null;
        }
        try {
            if (mInputStream != null) {
                mInputStream.close();
                mInputStream = null;
            }
            if (mOutputStream != null) {
                mOutputStream.close();
                mOutputStream = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (DNASerialPort != null) {
            DNASerialPort.close();
            DNASerialPort = null;
        }
    }


    @Override
    public boolean isConnected() {
        if (mOutputStream != null && mInputStream != null) {
            return true;
        }
        return false;
    }

    public void reOpen() {
        synchronized (OBDManager.class) {
            try {
                close();
                open();
            } catch (IOException e) {
                e.printStackTrace();
                Logs.e("reopen failed.");
            }
        }
    }

    private void sendData(byte[] data) throws IOException {
        if (mOutputStream == null)
            throw new IOException("not opened.");
        Logs.d("发给串口的数据<<<<<<<<" + UtilityTools.bytesToHexString(data));
        mOutputStream.write(data);
        mOutputStream.flush();
    }

    public void sendDataPackage(byte[] data) throws IOException {
        Message msg = dealHandler.obtainMessage(1);
        msg.obj = data;
        dealHandler.sendMessage(msg);
    }

    class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            DataInputStream dInput = new DataInputStream(mInputStream);
            final byte[] content = new byte[1024];
            int read;
            int head;
            while (!interrupted()) {
                try {
                    // 获取内容

                    read = dInput.read(content);
                    while (read != -1) {
                        byte[] temp = new byte[read];
                        System.arraycopy(content, 0, temp, 0, read);
                        if (callback != null)
                            callback.onReceive(temp);
                        read = dInput.read(content);
                    }
                    throw new IOException("read data end");
                } catch (IOException e) {
                    Logs.d("io error in receiving:  " + e.getMessage());
                    if (callback != null) {
                        callback.onError(e);
                        //reOpen();
                    }
                    dealException();
                    if (mInputStream != null) {
                        dInput = new DataInputStream(mInputStream);
                        if (callback != null) {
                            callback.onStart();
                        }
                    } else {
                        close();
                    }
                }

            }
        }
    }
}
