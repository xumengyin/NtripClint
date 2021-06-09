package com.xu.ntripclint.utils;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public class FileLogUtils {
    public static final String dirLoc="/zNtripLocLog/";
    public static final String wifiDirLoc="/zNtripLocLog/netLog/";
	private static String TAG = "FileLogUtils";
	private static Boolean MYLOG_SWITCH=true; // 日志文件总开关
    private static Boolean MYLOG_WRITE_TO_FILE=false;// 日志写入文件开关
    private static String MYLOG_PATH_SDCARD_DIR= Environment.getExternalStorageDirectory().getAbsolutePath()+dirLoc;// 日志文件在sdcard中的路径
    private static String WIFILOG_PATH_SDCARD_DIR= Environment.getExternalStorageDirectory().getAbsolutePath()+wifiDirLoc;
    private static int SDCARD_LOG_FILE_SAVE_DAYS = 5;// sd卡中日志文件的最多保存天数
    private static String MYLOGFILEName = "MyLog.txt";// 本类输出的日志文件名称
    private static String WIFILOGFILEName = "wifiLog.txt";// 本类输出的日志文件名称
    private static SimpleDateFormat myLogSdf = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");// 日志的输出格式  
    private static SimpleDateFormat logfile = new SimpleDateFormat("yyyy-MM-dd");// 日志文件格式
    private static SimpleDateFormat debuglogfile = new SimpleDateFormat("yyyyMMdd");// 日志文件格式



    /**
     * 保存错误信息到文件中
     *
     * @param ex
     * @return  返回文件名称,便于将文件传送到服务器
     */
    private static String saveCrashInfo2File(Throwable ex) {

        StringBuffer sb = new StringBuffer();

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        return sb.toString();
    }
    /** 
     * 记录网络日志
     *  
     * @return 
     * **/  
    public static void writeWifiLogtoFile( String text,Throwable throwable) {

        Logs.d("---写网络错误日志");
        Date nowtime = new Date();
        String needWriteFiel = logfile.format(nowtime);
        String needWriteMessage = myLogSdf.format(nowtime) + "  " + text;
        File file = new File(WIFILOG_PATH_SDCARD_DIR, needWriteFiel
                + WIFILOGFILEName);
        if(!file.getParentFile().exists())
        	file.getParentFile().mkdirs();
        try {  
            FileWriter filerWriter = new FileWriter(file, true);
            //BufferedWriter bufWriter = new BufferedWriter(filerWriter);
            BufferedWriter bufWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file,true), StandardCharsets.UTF_8));
            bufWriter.write(needWriteMessage);
            bufWriter.newLine();
            if(throwable!=null)
            {
                bufWriter.write(saveCrashInfo2File(throwable));
                bufWriter.newLine();
            }
            bufWriter.close();  
            filerWriter.close();  
        } catch (IOException e) {
        	Logs.e("保存日志失败");
            e.printStackTrace();  
        }  
    }  
    /**
     * 打开日志文件并写入日志
     *
     * @return
     * **/
    public static void writeLogtoFile( String text) {// 新建或打开日志文件
        Logs.d("---写LogtoFile日志");
        Date nowtime = new Date();
        String needWriteFiel = logfile.format(nowtime);
        String needWriteMessage = myLogSdf.format(nowtime) + "  " + text;
        File file = new File(MYLOG_PATH_SDCARD_DIR, needWriteFiel
                + MYLOGFILEName);
        if(!file.getParentFile().exists())
        	file.getParentFile().mkdirs();
        try {
            FileWriter filerWriter = new FileWriter(file, true);//后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖
            BufferedWriter bufWriter = new BufferedWriter(filerWriter);
            bufWriter.write(needWriteMessage);
            bufWriter.newLine();
            bufWriter.close();
            filerWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
        	Logs.e("保存日志失败");
            e.printStackTrace();
        }
    }

    
    /** 
     * 打开日志文件并写入日志 
     *  
     * @return 
     * **/  
    public static void writeDebugLogtoFile(String mylogtype, String tag, String text) {// 新建或打开日志文件
        Date nowtime = new Date();
        String needWriteFiel = debuglogfile.format(nowtime);
        String needWriteMessage = myLogSdf.format(nowtime) + "  " + mylogtype
                + "    " + tag + "    " + text;
        File file = new File(MYLOG_PATH_SDCARD_DIR, "debug-"+needWriteFiel
                + MYLOGFILEName);  
        if(!file.getParentFile().exists())
        	file.getParentFile().mkdirs();
        try {  
            FileWriter filerWriter = new FileWriter(file, true);//后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖
            BufferedWriter bufWriter = new BufferedWriter(filerWriter);
            bufWriter.write(needWriteMessage);  
            bufWriter.newLine();  
            bufWriter.close();  
            filerWriter.close();  
        } catch (IOException e) {
            // TODO Auto-generated catch block  
        	Logs.e("保存日志失败");
            e.printStackTrace();  
        }  
    }
    
    private static void writeLogtoFile(String mylogtype, String tag, String text, Throwable e1) {// 新建或打开日志文件
        Date nowtime = new Date();
        String needWriteFiel = logfile.format(nowtime);
        String needWriteMessage = myLogSdf.format(nowtime) + "    " + mylogtype
                + "    " + tag + "    " + text;  
        File file = new File(MYLOG_PATH_SDCARD_DIR, needWriteFiel
                + MYLOGFILEName);  
        if(!file.getParentFile().exists())
        	file.getParentFile().mkdirs();
        try {  
            FileWriter filerWriter = new FileWriter(file, true);//后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖
            BufferedWriter bufWriter = new BufferedWriter(filerWriter);
            bufWriter.write(needWriteMessage);  
            bufWriter.newLine();  
            StringBuffer exceptionStr = new StringBuffer();
            StackTraceElement[] elements = e1.getStackTrace();
            for (int i = 0; i < elements.length; i++) {  
                exceptionStr.append( myLogSdf.format(nowtime) + "    " + mylogtype  
                        + "    " + tag + " "+elements[i].toString() + "\n");  
            } 
            bufWriter.write(exceptionStr.toString());
            bufWriter.close();  
            filerWriter.close();  
        } catch (IOException e) {
            // TODO Auto-generated catch block  
        	Logs.e(TAG,"保存日志失败");
            e.printStackTrace();  
        }  
    } 
  
    /** 
     * 删除制定的日志文件 
     * */  
    public static void delFile() {// 删除日志文件  
        String needDelFiel = logfile.format(getDateBefore());
        File file = new File(MYLOG_PATH_SDCARD_DIR, needDelFiel + MYLOGFILEName);
        if (file.exists()) {  
            file.delete();  
        }  
    }  
  
    /** 
     * 得到现在时间前的几天日期，用来得到需要删除的日志文件名 
     * */  
    private static Date getDateBefore() {
        Date nowtime = new Date();
        Calendar now = Calendar.getInstance();
        now.setTime(nowtime);  
        now.set(Calendar.DATE, now.get(Calendar.DATE)
                - SDCARD_LOG_FILE_SAVE_DAYS);  
        return now.getTime();  
    } 

}
