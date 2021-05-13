package com.xu.ntripclint.activity

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cpsdna.obdports.ports.OBDManager
import com.tbruyelle.rxpermissions3.RxPermissions
import com.xu.ntripclint.IServiceCallBack
import com.xu.ntripclint.MainActivity
import com.xu.ntripclint.R
import com.xu.ntripclint.WorkService
import com.xu.ntripclint.network.NetManager
import com.xu.ntripclint.ntrip.NtripManager
import com.xu.ntripclint.utils.Logs
import com.xu.ntripclint.utils.Storage
import kotlinx.android.synthetic.main.activity_splash.*

class SplashActivity : AppCompatActivity() {

    val rxPermissions = RxPermissions(this@SplashActivity)
    var serviceBinder: IBinder? = null
    var service: WorkService? = null
    var demonServiceStart = false
    val mainHandler = Handler(Looper.getMainLooper())
    val conn = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            Logs.d("onServiceDisconnected")
        }

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            binder?.apply {
                serviceBinder = this
                service = (serviceBinder as WorkService.WorkBinder).service
                //更新一次状态
                updateView()
                service?.switchUploadGpgga(false)
                service?.setMockUpload(false)
                service?.setServiceCallBack(object : IServiceCallBack {
                    override fun onSerialPortStatus(status: Int) {
                        runOnUiThread {
                            val msg =
                                if (status != IServiceCallBack.STATUS_OK) "串口:连接失败" else "串口:连接ok"
                            vSerialPort.text = msg
                        }

                    }

                    override fun onNmeaRecieve(
                        data: String?,
                        lat: Double,
                        lng: Double,
                        gpsS: String
                    ) {
                        data?.apply {
                            //解析 经纬度展示
                            vLatlng.text = "${lat},${lng}"
                        }
                    }

                    override fun onNetStatus(status: Int) {
                        runOnUiThread {
                            val msg =
                                if (status != IServiceCallBack.STATUS_OK) "上传服务器:连接失败" else "上传服务器:连接ok"
                            vUploadServer.text = msg
                        }

                    }

                    override fun onNtripStatus(status: Int, errorStr: String?) {

                        runOnUiThread {
                            val msg =
                                if (status != IServiceCallBack.STATUS_OK) "ntrip服务:连接失败" else "ntrip服务:连接ok"
                            vNtrip.text = msg
                            if (!TextUtils.isEmpty(errorStr)) {
                                Toast.makeText(this@SplashActivity, errorStr, Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }

                    }

                    override fun ntripDebugData(data: String?) {
//                        runOnUiThread {
//                            //vDebugText.setText(data)
//                        }

                    }

                })
            }
        }
    }

    fun updateView() {
        val dataBean = Storage.getData(this)
        if (OBDManager.getInstance(applicationContext).isConnected) {
            vSerialPort.text = "串口连接ok"
        } else {
            vSerialPort.text = "串口连接失败"
        }
        if (dataBean.ntripServer == dataBean.uploadServer && dataBean.ntripServerPort == dataBean.uploadPort) {
            if (NtripManager.getInstance().isConnectedNtrip) {
                vUploadServer.text = "上传服务器连接ok"
            } else {
                vUploadServer.text = "上传服务器连接失败"
            }
        } else {
            if (NetManager.getInstance(applicationContext).isStarted) {
                vUploadServer.text = "上传服务器连接ok"
            } else {
                vUploadServer.text = "上传服务器连接失败"
            }
        }
        if (NtripManager.getInstance().isConnectedNtrip) {
            vNtrip.text = "ntrip服务连接ok"
        } else {
            vNtrip.text = "ntrip服务:连接失败"
        }
    }

    private fun initView() {
        vSetting.setOnClickListener {
            val intent = Intent(this@SplashActivity,MainActivity::class.java)
            startActivity(intent)
            this@SplashActivity.finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        initView()
        rxPermissions.request(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).subscribe { grant ->
            if (grant) {
                createService()
                intent.getBooleanExtra(WorkService.SELF_START, false).apply {

                    if (this) {
                        mainHandler.postDelayed({
                            this@SplashActivity.finish()
                        }, 5000)
                    }
                }
            } else {
                finish()
            }
        }
    }

    private fun createService() {
        val intent = Intent(this@SplashActivity, WorkService::class.java)
        val intent2 = Intent(this@SplashActivity, WorkService::class.java)
        intent.putExtra(WorkService.START_TAG, true)
//        if (isDeamon) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                startForegroundService(intent)
//            } else {
        startService(intent)
//            }
//            demonServiceStart=true
//        }
        bindService(intent2, conn, Context.BIND_AUTO_CREATE)
    }

    private fun destroyService() {
        unbindService(conn)
    }


    override fun onDestroy() {
        super.onDestroy()
        destroyService()
    }
}