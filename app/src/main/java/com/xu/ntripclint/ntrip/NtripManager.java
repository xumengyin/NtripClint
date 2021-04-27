package com.xu.ntripclint.ntrip;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.xu.ntripclint.pojo.ConfigBean;
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
    public static final int TEST_PORT = 8001;
    public static final String TEST_USERNAME = "";
    public static final String TEST_USERPASS = "";
    public static final String TEST_MOUNT = "";

    //118.809563,32.023878
    private Double ManualLat = 32.023878;
    private Double ManualLon = 118.809563;

    private static final int MSG_NETWORK_GOT_DATA = 101;
    private static final int MSG_NETWORK_TIMEOUT = 198;
    private static final int MSG_NETWORK_FINISHED = 199;
    private static final int MSG_NETWORK_SEND_DATA = 200;

    static {
        manager = new NtripManager();
    }

    static NtripManager manager;
    Thread nThread;
    Socket nsocket; // Network Socket
    InputStream nis = null; // Network Input Stream
    OutputStream nos = null; // Network Output Stream
    String server = TEST_SERVER, userName = TEST_USERNAME, userPass = TEST_USERPASS, mountPoint = TEST_MOUNT;
    int port = TEST_PORT;
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
    volatile boolean NetworkIsConnected = false;

    public static NtripManager getInstance() {
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

//    public void setGgaData(String ggaData) {
//        this.ggaData = ggaData;
//    }

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
            case MSG_NETWORK_SEND_DATA:
                if(callBack!=null)
                {
                    callBack.onReceive((byte[]) msg.obj);
                }
                break;
            case MSG_NETWORK_TIMEOUT:
                break;
            case MSG_NETWORK_FINISHED:
                NetworkIsConnected = false;
                callBack.onDisConnect("");
                break;

        }
    }
    private String GenerateGGAFromLatLon() {
        String gga = "GPGGA,000001,";

        double posnum = Math.abs(ManualLat);
        double latmins = posnum % 1;
        int ggahours = (int)(posnum - latmins);
        latmins = latmins * 60;
        double latfracmins = latmins % 1;
        int ggamins = (int)(latmins - latfracmins);
        int ggafracmins = (int)(latfracmins * 10000);
        ggahours = ggahours * 100 + ggamins;
        if (ggahours < 1000) {
            gga += "0";
            if (ggahours < 100) {
                gga += "0";
            }
        }
        gga += ggahours + ".";
        if (ggafracmins < 1000) {
            gga += "0";
            if (ggafracmins < 100) {
                gga += "0";
                if (ggafracmins < 10) {
                    gga += "0";
                }
            }
        }
        gga += ggafracmins;
        if (ManualLat > 0) {
            gga += ",N,";
        } else {
            gga += ",S,";
        }

        posnum = Math.abs(ManualLon);
        latmins = posnum % 1;
        ggahours = (int)(posnum - latmins);
        latmins = latmins * 60;
        latfracmins = latmins % 1;
        ggamins = (int)(latmins - latfracmins);
        ggafracmins = (int)(latfracmins * 10000);
        ggahours = ggahours * 100 + ggamins;
        if (ggahours < 10000) {
            gga += "0";
            if (ggahours < 1000) {
                gga += "0";
                if (ggahours < 100) {
                    gga += "0";
                }
            }
        }
        gga += ggahours + ".";
        if (ggafracmins < 1000) {
            gga += "0";
            if (ggafracmins < 100) {
                gga += "0";
                if (ggafracmins < 10) {
                    gga += "0";
                }
            }
        }
        gga += ggafracmins;
        if (ManualLon > 0) {
            gga += ",E,";
        } else {
            gga += ",W,";
        }

        gga += "1,8,1,0,M,-32,M,3,0";

        String checksum = CalculateChecksum(gga);

        //Log.i("Manual GGA", "$" + gga + "*" + checksum);
        return "$" + gga + "*" + checksum;
    }
    private String CalculateChecksum(String line) {
        int chk = 0;
        for (int i = 0; i < line.length(); i++) {
            chk ^= line.charAt(i);
        }
        String chk_s = Integer.toHexString(chk).toUpperCase(); // convert the integer to a HexString in upper case
        while (chk_s.length() < 2) { // checksum must be 2 characters. if it falls short, add a zero before the checksum
            chk_s = "0" + chk_s;
        }
        return chk_s;
    }
    private void SendGGAToCaster(String ggaData) {
//        if (UseManualLocation) {
//            SendDataToNetwork(GenerateGGAFromLatLon() + "\r\n");
//        } else {
//            SendDataToNetwork(MostRecentGGA + "\r\n");
//        }
        if(!TextUtils.isEmpty(ggaData)&&NetworkDataMode==99)
        {
            Logs.w("send gpa to ntrip:"+ggaData);
            SendDataToNetwork(ggaData+"\r\n");
        }
    }
    public void SendDataToNetwork(String cmd) {
        try {
            if (nsocket != null) {
                if (nsocket.isConnected()) {
                    if (!nsocket.isClosed()) {
                        //Log.i("SendDataToNetwork", "SendDataToNetwork: Writing message to socket");
                        nos.write(cmd.getBytes());
                        nos.flush();
                    } else {
                        //Log.i("SendDataToNetwork", "SendDataToNetwork: Cannot send message. Socket is closed");
                    }
                } else {
                    //Log.i("SendDataToNetwork", "SendDataToNetwork: Cannot send message. Socket is not connected");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            //Log.i("SendDataToNetwork", "SendDataToNetwork: Message send failed. Caught an exception");
        }
    }
    private int NetworkDataMode = 0;
    private String NTRIPResponse = "";

    private void parseNetworkDataStream(byte[] buffer) {
        if (NetworkDataMode == 0) {
            NTRIPResponse += new String(buffer);
            if (NTRIPResponse.startsWith("ICY 200 OK")) {
                callBack.onConnected();
//                if (NTRIPStreamRequiresGGA) {
                NetworkDataMode = 99; // Put in to data mode
                // todo  test
                  // SendGGAToCaster(GenerateGGAFromLatLon());
//                }

                Logs.d("NTRIP: Connected to caster");
            } else if (NTRIPResponse.indexOf("401 Unauthorized") > 1) {
                //Log.i("handleMessage", "Invalid Username or Password.");
                Logs.d("NTRIP: Bad username or password.");
                TerminateNTRIPThread("NTRIP用户名密码不正确");
            } else if (NTRIPResponse.startsWith("SOURCETABLE 200 OK")) {
                Logs.d("NTRIP: Downloading stream list");
                NetworkDataMode = 1; // Put into source table mode
                //NTRIPResponse = NTRIPResponse.substring(20); // Drop the beginning of the data
                // CheckIfDownloadedSourceTableIsComplete();
            } else if (NTRIPResponse.length() > 1024) { // We've received 1KB of data but no start command. WTF?
                Logs.d("NTRIP: Unrecognized server response:");
                Logs.d(NTRIPResponse);
                TerminateNTRIPThread("NTRIP接收文件错误");
            }
        } else if (NetworkDataMode == 1) { // Save SourceTable
            NTRIPResponse += new String(buffer);
            //CheckIfDownloadedSourceTableIsComplete();
        } else { // Data streaming mode. Forward data to bluetooth socket
            //NetworkReceivedByteCount += buffer.length;
//            SendByteCountToActivity();

            //todo 给串口
            //SendDataToBluetooth(buffer);
            Message msg=mainHandler.obtainMessage(MSG_NETWORK_SEND_DATA);
            msg.obj=buffer;
            mainHandler.sendMessage(msg);
        }
    }

    private void TerminateNTRIPThread(String error) {
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
            callBack.onDisConnect(error);
            NetworkDataMode=0;
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

    @Override
    public void setServer(ConfigBean bean) {
        server = bean.ntripServer;
        port = bean.ntripServerPort;
        mountPoint = bean.ntripServerMount;
        userName = bean.userName;
        userPass = bean.password;
    }
    NtripCallBack callBack;
    @Override
    public void setCallBack(NtripCallBack callBack) {
        this.callBack=callBack;
    }

    @Override
    public void sendGPGGA(String gpgga) {
        if(!TextUtils.isEmpty(gpgga))
        {
            SendGGAToCaster(gpgga);
        }
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
        if (!NetworkIsConnected && !TextUtils.isEmpty(server)) {
            Runnable clint = new NetworkClient(server, port, mountPoint, userName, userPass);
            nThread = new Thread(clint);
            nThread.start();
            NetworkIsConnected = true;
            NetworkDataMode=0;
        }
    }

    public boolean isNetworkIsConnected() {
        return NetworkIsConnected;
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
    public void disconnectServer() {
        cancleSechdule();
        TerminateNTRIPThread("正常关闭ntrip连接");
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
                Logs.w("NetworkClient start server:"+nServer+"--port:"+port);
                SocketAddress sockaddr = new InetSocketAddress(nServer, nPort);
                nsocket = new Socket();
                nsocket.connect(sockaddr, 20 * 1000); // 10 second connection timeout
                if (nsocket.isConnected()) {
                    nsocket.setSoTimeout(0); // 20 second timeout once data is flowing
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
