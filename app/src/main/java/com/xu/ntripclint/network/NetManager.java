package com.xu.ntripclint.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.cpsdna.obdports.ports.UtilityTools;
import com.xu.ntripclint.utils.Logs;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import javax.net.SocketFactory;

/**
 * 网络上报
 */
public final class NetManager {
    private volatile boolean isStarted = false;
    private volatile boolean isConnecting = false;
    private volatile boolean shouldReconnect = false;
    private volatile boolean shouldQuit = false;

    private DataOutputStream output = null;
    private DataInputStream input = null;

    private static final byte[] LOCKER_START = new byte[0]; // 同步锁
    private static final byte[] LOCKER_WRITE = new byte[0]; // 同步锁

    private int count = 0; // 包序列

    private String ip = "";
    private int port = 1000;
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            //顺序启动socket
            handleStart();
        }
    };

    int isFristData = 1;
    private NetCallback netCallback = null;

    public Context mContext;

    private static NetManager ourInstance;
    Socket client = null;

    int nameCount = 1;

    public static NetManager getInstance(Context context) {
        if (ourInstance == null) {
            ourInstance = new NetManager(context.getApplicationContext());
        }
        return ourInstance;
    }

    private NetManager(Context context) {
        mContext = context;
    }


    public void setConfig(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void start(NetCallback callback) {
        if (callback == null)
            throw new NullPointerException();
        if (TextUtils.isEmpty(ip)) {
            Toast.makeText(mContext, "上报地址为空", Toast.LENGTH_SHORT).show();
            return;
        }
        netCallback = callback;
        sendStartMsg(0);
    }

    private void sendStartMsg(long delay) {
        handler.removeCallbacksAndMessages(null);
        if (delay > 0)
            handler.sendEmptyMessageDelayed(11, delay);
        else
            handler.sendEmptyMessage(11);
    }

    public boolean isStarted() {
        return isStarted;
    }

    private void handleStart() {
        boolean shouldStart = isStarted;
        shouldQuit = false;
        shouldReconnect=false;
        if (!shouldStart && !isConnecting) {
            startImpl();
        } else {
            reConnect();
        }
        nameCount++;
    }

    public void close() {
        shouldQuit = true;
        closeAll();
    }

    private void startImpl() {
        isConnecting = true;
        new Thread("netManager" + nameCount) {
            public void run() {
                // Timer timer = new Timer();
                try {
                    closeAll();
                    client = SocketFactory.getDefault().createSocket();
                    Logs.w("连接到服务器..." + Thread.currentThread());
                    SocketAddress remoteaddr = new InetSocketAddress(ip, port);
                    Logs.w("连接..." + ip + "..." + port);
                    client.connect(remoteaddr, 20000); // 连接20秒超时
                    client.setSoTimeout(0); // 读写15秒
                    Logs.w("已连接，开始监听...");
                    netCallback.onConnected();
                    output = new DataOutputStream(
                            client.getOutputStream());
                    input = new DataInputStream(
                            client.getInputStream());
                    isStarted = true;
                    isConnecting = false;
                    if (shouldReconnect) {
                        throw new Exception("需要重连");
                    }
                    byte[] temp = new byte[2048];
                    int read;
                    while ((read = input.read(temp)) != -1) {
                        byte[] tempData = new byte[read];
                        System.arraycopy(temp, 0, tempData, 0, read);
                        netCallback.onReceive(tempData);
                    }
                    throw new Exception("net 读到-1");
                    //shouldReconnect=true;
                } catch (Exception e) {
                    Logs.e("net Manager Exception 稍后重连 e:" + e.getMessage());
                    shouldReconnect = true;
                } finally {
//                    timer.cancel();
                    closeAll();
                    if (shouldReconnect && !shouldQuit) {
                        shouldReconnect = false;
                        sendStartMsg(2000); //2s后重连
                        // startImpl();
                    }
                }
            }
        }.start();
    }


    public void reConnect() {
        shouldReconnect = true;
        try {
            if (output != null)
                output.close();
            if (input != null)
                input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeAll() {
        try {
            isStarted = false;
            if (output != null) {
                output.close();
                output = null;
            }
            if (input != null) {
                input.close();
                input = null;
            }
            if (client != null) {
                client.close();
                client = null;
            }
            netCallback.ondisConnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
        上报数据
     */
    public void writeDirect(String gpgga, int battery) throws IOException {
        if (output == null)
            return;
        synchronized (LOCKER_WRITE) {

            // output.write(data);
            if (count > 255) {
                count = 0;
            }
            String buffer = gpgga + "0" +
                    UtilityTools.byteToHex(count) +
                    UtilityTools.byteToHex(battery) +
                    isFristData + "";
            Logs.d("net manager upload ----"+buffer);
            output.write(buffer.getBytes());
            isFristData = 0;
            count++;
        }
    }
}
