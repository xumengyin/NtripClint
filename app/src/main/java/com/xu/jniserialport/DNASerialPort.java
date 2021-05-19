/*
 * Copyright 2009 Cedric Priscal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xu.jniserialport;

import com.xu.ntripclint.utils.Logs;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DNASerialPort {

    /*
     * Do not remove or rename the field mFd: it is used by native method
     * close();
     */
    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;
    private String systemBin[]={"/system/bin/su","/system/xbin/su"};
    public DNASerialPort(File device, int baudrate, int flags)
            throws SecurityException, IOException {

        if (!device.canRead() || !device.canWrite()) {
            boolean execOk=false;
            for (int i = 0; i < systemBin.length; i++)
            {
                try {
                    Process su;
                    su = Runtime.getRuntime().exec(systemBin[i]);
                    String cmd = "chmod 666 " + device.getAbsolutePath() + "\n"
                            + "exit\n";
                    su.getOutputStream().write(cmd.getBytes());
                    if ((su.waitFor() != 0) || !device.canRead()
                            || !device.canWrite()) {
                        // throw new SecurityException();
                        continue;
                    }
                    execOk=true;
                    Logs.e(systemBin[i]+"execute success");
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    Logs.e(systemBin[i]+"execute fail");
                }
            }
            if(!execOk)
            {
                Logs.e("can not execute all");
                throw new SecurityException();
            }
        }

        mFd = open(device.getAbsolutePath(), baudrate, flags);
        if (mFd == null) {
            Logs.e("native open returns null");
            throw new IOException();
        }
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);
    }

    // Getters and setters
    public InputStream getInputStream() {
        return mFileInputStream;
    }

    public OutputStream getOutputStream() {
        return mFileOutputStream;
    }

    // JNI
    private native static FileDescriptor open(String path, int baudrate,
                                              int flags);

    public native void close();

    static {
        try {
            System.loadLibrary("DSerialPort");
        } catch (Throwable var1) {
            var1.printStackTrace();
        }
    }
}
