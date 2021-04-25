package com.xu.ntripclint.ntrip;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.xu.ntripclint.utils.Logs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

public class NtripManager implements INtrip {
    public static final String TEST_SERVER = "";
    public static final String TEST_PORT = "";
    public static final String TEST_USERNAME = "";
    public static final String TEST_USERPASS = "";
    public static final String TEST_MOUNT = "";


    private static final int MSG_NETWORK_GOT_DATA = 101;
    private static final int MSG_NETWORK_TIMEOUT = 198;
    private static final int MSG_NETWORK_FINISHED = 199;

    static {
        manager = new NtripManager();
    }

    static NtripManager manager;
    Thread nThread;
    Socket nsocket; // Network Socket
    InputStream nis = null; // Network Input Stream
    OutputStream nos = null; // Network Output Stream
    String server = TEST_SERVER, port = TEST_PORT, userName = TEST_USERNAME, userPass = TEST_USERPASS, mountPoint = TEST_MOUNT;

    private Timer timer;
    private TimerTask timerTask;
    HandlerThread handlerThread = new HandlerThread("ntrpHandler");

    Handler dataHandler;
    Handler mainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            handlerMainThreadMsg(msg);
        }
    };
    boolean NetworkIsConnected = false;

    public NtripManager getInstance() {
        return manager;
    }

    private NtripManager() {
        handlerThread.start();
        dataHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                handlerMsg(msg);
            }
        };
    }

    private void handlerMsg(Message msg) {
        switch (msg.what) {

            case MSG_NETWORK_GOT_DATA:
                byte[] buffer1 = (byte[]) msg.obj;
                parseNetworkDataStream(buffer1);
                break;
        }
    }

    private void handlerMainThreadMsg(Message msg) {
        switch (msg.what) {
            case MSG_NETWORK_TIMEOUT:
            case MSG_NETWORK_FINISHED:
                NetworkIsConnected = false;
                break;

        }
    }

    private int NetworkDataMode = 0;
    private String NTRIPResponse = "";

    private void parseNetworkDataStream(byte[] buffer) {
        if (NetworkDataMode == 0) {
            NTRIPResponse += new String(buffer);
            if (NTRIPResponse.startsWith("ICY 200 OK")) {

//                if (NTRIPStreamRequiresGGA) {
//                    SendGGAToCaster();
//                }
                NetworkDataMode = 99; // Put in to data mode
                Logs.d("NTRIP: Connected to caster");
            } else if (NTRIPResponse.indexOf("401 Unauthorized") > 1) {
                //Log.i("handleMessage", "Invalid Username or Password.");
                Logs.d("NTRIP: Bad username or password.");
                TerminateNTRIPThread();
            } else if (NTRIPResponse.startsWith("SOURCETABLE 200 OK")) {
                Logs.d("NTRIP: Downloading stream list");
                NetworkDataMode = 1; // Put into source table mode
                //NTRIPResponse = NTRIPResponse.substring(20); // Drop the beginning of the data
                // CheckIfDownloadedSourceTableIsComplete();
            } else if (NTRIPResponse.length() > 1024) { // We've received 1KB of data but no start command. WTF?
                Logs.d("NTRIP: Unrecognized server response:");
                Logs.d(NTRIPResponse);
                TerminateNTRIPThread();
            }
        } else if (NetworkDataMode == 1) { // Save SourceTable
            NTRIPResponse += new String(buffer);
            //CheckIfDownloadedSourceTableIsComplete();
        } else { // Data streaming mode. Forward data to bluetooth socket
            //NetworkReceivedByteCount += buffer.length;
//            SendByteCountToActivity();

            //todo 给串口
            //SendDataToBluetooth(buffer);
        }
    }

    private void TerminateNTRIPThread() {
        if (nThread != null) { // If the thread is currently running, close the socket and interrupt it.
            try {
                if (nis != null)
                    nis.close();
                if (nos != null)
                    nos.close();
                if (nsocket != null)
                    nsocket.close();
            } catch (Exception e) {
            }
            Thread moribund = nThread;
            nThread = null;
            moribund.interrupt();
        }
        //NTRIPShouldBeConnected = restart;
//        if (restart) {
//            NetworkReConnectInTicks = 2;
//        } else {
//            NetworkProtocol = "none"; //Don't automatically restart
//        }
    }

    @Override
    public void connectServer() {
        disconnectServer();
        sechdule();
    }

    private void sechdule() {
        cancleSechdule();
        timer = new Timer();
        timerTask = new TimerTask() {
            public void run() {
                // onTimerTick_TimerThread();
                onTimerTick();
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, 1000L);
    }

    private void onTimerTick() {
        if (!NetworkIsConnected) {
            Runnable clint = new NetworkClient(server, Integer.parseInt(port), mountPoint, userName, userPass);
            nThread = new Thread(clint);
            nThread.start();
        }
    }

    private void cancleSechdule() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void setServer(String server, String port, String mountPoint, String name, String pass) {

    }

    @Override
    public void disconnectServer() {
        cancleSechdule();
        TerminateNTRIPThread();
    }


    public class NetworkClient implements Runnable {
        String nProtocol = "";
        String nServer = "";
        int nPort = 2101;
        String nMountpoint = "";
        String nUsername = "";
        String nPassword = "";

        public NetworkClient(String pServer, int pPort, String pMountpoint, String pUsername, String pPassword) {
            nServer = pServer;
            nPort = pPort;
            nMountpoint = pMountpoint;
            nUsername = pUsername;
            nPassword = pPassword;
        }

        public void run() {
            try {
                //Log.i(NTAG, "Creating socket");
                SocketAddress sockaddr = new InetSocketAddress(nServer, nPort);
                nsocket = new Socket();
                nsocket.connect(sockaddr, 10 * 1000); // 10 second connection timeout
                if (nsocket.isConnected()) {
                    nsocket.setSoTimeout(20 * 1000); // 20 second timeout once data is flowing
                    nis = nsocket.getInputStream();
                    nos = nsocket.getOutputStream();
                    //Log.i(NTAG, "Socket created, streams assigned");


                    // Build request message
                    //Log.i(NTAG, "This is a NTRIP connection");
                    String requestmsg = "GET /" + nMountpoint + " HTTP/1.0\r\n";
                    requestmsg += "User-Agent: NTRIP LefebureAndroidNTRIPClient/20120614\r\n";
                    requestmsg += "Accept: */*\r\n";
                    requestmsg += "Connection: close\r\n";
                    if (nUsername.length() > 0) {
                        requestmsg += "Authorization: Basic " + ToBase64(nUsername + ":" + nPassword);
                    }
                    requestmsg += "\r\n";
                    nos.write(requestmsg.getBytes());
                    //Log.i("Request", requestmsg);


                    //Log.i(NTAG, "Waiting for inital data...");
                    byte[] buffer = new byte[4096];
                    int read = nis.read(buffer, 0, 4096); // This is blocking
                    while (read != -1) {
                        byte[] tempdata = new byte[read];
                        System.arraycopy(buffer, 0, tempdata, 0, read);
                        // Log.i(NTAG, "Got data: " + new String(tempdata));
                        dataHandler.sendMessage(dataHandler.obtainMessage(MSG_NETWORK_GOT_DATA, tempdata));
                        read = nis.read(buffer, 0, 4096); // This is blocking
                    }
                }
            } catch (SocketTimeoutException ex) {
                ex.printStackTrace();
                mainHandler.sendMessage(mainHandler.obtainMessage(MSG_NETWORK_TIMEOUT));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    nis.close();
                    nos.close();
                    nsocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //Log.i(NTAG, "Finished");
                mainHandler.sendMessage(mainHandler.obtainMessage(MSG_NETWORK_FINISHED));
            }
        }

        private String ToBase64(String in) {
            return Base64.encodeToString(in.getBytes(), 4);
        }
    }
}
