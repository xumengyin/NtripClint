/**
 *
 */
package com.xu.ntripclint.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;

import com.xu.ntripclint.pojo.ConfigBean;

import java.lang.reflect.Field;

/**
 * @author w
 */
public class Storage {

    public static String KEY_NTRIP_SERVER = "KEY_NTRIP_SERVER";
    public static String KEY_NTRIP_PORT = "KEY_NTRIP_PORT";
    public static String KEY_NTRIP_MOUNT = "KEY_NTRIP_MOUNT";
    public static String KEY_NTRIP_USERNAME = "KEY_NTRIP_USERNAME";
    public static String KEY_NTRIP_PASS = "KEY_NTRIP_PASS";
    public static String KEY_UPLOAD_SERVER = "KEY_UPLOAD_SERVER";
    public static String KEY_UPLOAD_PORT = "KEY_UPLOAD_PORT";
    public static String KEY_UPLOAD_TIME = "KEY_UPLOAD_TIME";


    public static String DEFAULT_DEVICE = "";
    public static int DEFALUT_RATE = 19200;
    // 网关地址
    private static final String BASE_REMOTE_IP = "58.215.50.36";
    private static final int BASE_REMOTE_PORT = 4048;
    // 更新obd地址
    private static final String UPDATE_REMOTE_IP = "58.215.50.36";
    private static final int UPDATE_REMOTE_PORT = 4048;

    public static SharedPreferences getPre(Context context) {
        return context.getApplicationContext().getSharedPreferences("ntrip", 0);
    }

    public static void saveData(Context context, String server, int port, String mount, String userName, String pass, String uploadServer, int uploadTime) {
        SharedPreferences sp = getPre(context);
        Editor editor = sp.edit();
        editor.putString(KEY_NTRIP_SERVER, server);
        editor.putInt(KEY_NTRIP_PORT, port);
        editor.putString(KEY_NTRIP_MOUNT, mount);
        editor.putString(KEY_NTRIP_USERNAME, userName);
        editor.putString(KEY_NTRIP_PASS, pass);
        editor.putString(KEY_UPLOAD_SERVER, uploadServer);
        editor.putInt(KEY_UPLOAD_TIME, uploadTime);
        editor.commit();

    }

    public static void saveNtripData(Context context, String server, int port, String mount, String userName, String pass) {
        SharedPreferences sp = getPre(context);
        Editor editor = sp.edit();
        editor.putString(KEY_NTRIP_SERVER, server);
        editor.putInt(KEY_NTRIP_PORT, port);
        editor.putString(KEY_NTRIP_MOUNT, mount);
        editor.putString(KEY_NTRIP_USERNAME, userName);
        editor.putString(KEY_NTRIP_PASS, pass);
        editor.commit();

    }

    public static void saveUploadData(Context context, String uploadServer, int port,int uploadTime) {
        SharedPreferences sp = getPre(context);
        Editor editor = sp.edit();
        editor.putString(KEY_UPLOAD_SERVER, uploadServer);
        editor.putInt(KEY_UPLOAD_PORT, port);
        editor.putInt(KEY_UPLOAD_TIME, uploadTime);
        editor.commit();

    }

    public static ConfigBean getData(Context context) {
        SharedPreferences sp = getPre(context);

        return new ConfigBean(sp.getString(KEY_NTRIP_SERVER, "203.107.45.154"),
                sp.getInt(KEY_NTRIP_PORT, 8002),
                sp.getString(KEY_NTRIP_MOUNT, "AUTO"),
                sp.getString(KEY_NTRIP_USERNAME, "qxwfly001"),
                sp.getString(KEY_NTRIP_PASS, "bcafdc8"),
                sp.getString(KEY_UPLOAD_SERVER, ""),
                sp.getInt(KEY_UPLOAD_PORT, 3333),
                sp.getInt(KEY_UPLOAD_TIME, 2));
    }


}
