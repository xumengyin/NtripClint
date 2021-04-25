package com.cpsdna.obdports.ports;

import android.content.Context;

import com.xu.ntripclint.utils.Logs;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by W.
 */

public class SerialPortImpl extends AbstractPort {


    public static final String TEST_DEVICE = "/dev/ttyS0";
    public static final int TEST_BAUDRATE = 115200;

    Context mContext;
    DNASerialPort DNASerialPort;
    public OutputStream mOutputStream;
    InputStream mInputStream;
    ReadThread readThread;


    public SerialPortImpl(Context context) {
        mContext = context;
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
                readThread = new ReadThread();
                readThread.start();
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

    public void sendDataPackage(byte[] data) throws IOException {
        if (mOutputStream == null)
            throw new IOException("not opened.");
        Logs.d("发给OBD的数据<<<<<<<<" + UtilityTools.bytesToHexString(data));
        if (data == null) {
            return;
        }
        mOutputStream.write(data);
        mOutputStream.flush();
    }

    class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            DataInputStream dInput = new DataInputStream(mInputStream);
            while (!interrupted()) {
                try {
                    int first;
                    boolean isHead = false;
                    while (true) {
                        int b = dInput.read();
                        if (b == -1) {
                            //Log.i("CpsdnaLo", "-1");
                            //throw new IOException("data is at the end.");
                        }
                        if (isHead) {
                            if (b != 0x7e) {
                                first = b;
                                break;
                            }
                        } else {
                            if (b == 0x7e) {
                                isHead = true;
                                continue;
                            }
                        }
                    }
//                    if(debugTest)
//                    {
//                        debugTest=false;
//                        throw new IOException("debugTest--IOException");
//                    }
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    while (first != 0x7e) {
                        if (first == 0x7d) {
                            first = dInput.read();
                            if (first == -1) {
                                throw new IOException("data is at the end. 2");
                            }
                            if (first == 0x02) {
                                out.write(0x7e);
                                first = dInput.read();
                                if (first == -1) {
                                    throw new IOException("data is at the end. 3");
                                }
                            } else if (first == 0x01) {
                                out.write(0x7d);
                                first = dInput.read();
                                if (first == -1) {
                                    throw new IOException("data is at the end. 4");
                                }
                            } else {
                                out.write(0x7d);
                            }
                        } else {
                            out.write(first);
                            first = dInput.read();
                            if (first == -1) {
                                throw new IOException("data is at the end. 5");
                            }
                        }
                    }
                    final byte[] data = out.toByteArray();
                    int contentLength = data.length - 1 - 8;
                    if (contentLength < 0) {
                        Logs.d("data length is wrong,ignore...");
                        continue;
                    }
                    byte cs = 0x00;
                    for (int i = 0; i < data.length - 1; i++) {
                        cs = (byte) (cs ^ data[i]);
                    }
                    if (cs != data[data.length - 1]) {
                        Logs.d("checksum is wrong,ignore...");
                        continue;
                    }
                    // 获取ID
                    short id;
                    if (data.length > 1) {
                        id = (short) ((data[0] & 0xff) << 8 | data[1] & 0xff);
                    } else {
                        Logs.d("data length is wrong,ignore...");
                        continue;
                    }

                    // 获取内容
                    final byte[] content = new byte[contentLength];
                    for (int i = 0; i < content.length; i++) {
                        content[i] = data[8 + i];
                    }
                    if (callback != null)
                        callback.onReceive(id, content, data);
                } catch (IOException e) {
                    Logs.d("io error in receiving:  " + e.getMessage());
                    dealException();
                    if (mInputStream != null) {
                        dInput = new DataInputStream(mInputStream);
                    } else {
                        close();
                    }
                    if (callback != null) {
                        callback.onError(e);
                        //reOpen();
                    }
                }

            }
        }
    }
}
