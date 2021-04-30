package com.xu.ntripclint

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cpsdna.obdports.ports.OBDManager
import com.tbruyelle.rxpermissions3.RxPermissions
import com.xu.ntripclint.network.NetManager
import com.xu.ntripclint.ntrip.NtripManager
import com.xu.ntripclint.pojo.ConfigBean
import com.xu.ntripclint.utils.FileLogUtils
import com.xu.ntripclint.utils.Logs
import com.xu.ntripclint.utils.Storage
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val rxPermissions = RxPermissions(this@MainActivity)
    var serviceBinder: IBinder? = null
    var service: WorkService? = null
    var demonServiceStart = false
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
                                if (status != IServiceCallBack.STATUS_OK) "串口连接失败" else "串口连接ok"
                            vSerialPortStatus.text = msg
                        }

                    }

                    override fun onNmeaRecieve(data: String?, lat: Double, lng: Double,gpsS:String) {
                        data?.apply {
                            //解析 经纬度展示
                            vLat.text="经度:"+lat
                            vLng.text="纬度:"+lng
                            vGpsStatus.text=gpsS
                            vNmeaData.text=data
                        }
                    }


                    override fun onNetStatus(status: Int) {
                        runOnUiThread {
                            val msg =
                                if (status != IServiceCallBack.STATUS_OK) "上传服务器连接失败" else "上传服务器连接ok"
                            vUploadStatus.text = msg
                        }

                    }

                    override fun onNtripStatus(status: Int, errorStr: String?) {

                        runOnUiThread {
                            val msg =
                                if (status != IServiceCallBack.STATUS_OK) "ntrip连接失败" else "ntrip连接ok"
                            vNtripStatus.text = msg
                            if (!TextUtils.isEmpty(errorStr)) {
                                Toast.makeText(this@MainActivity, errorStr, Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }

                    }

                    override fun ntripDebugData(data: String?) {
                        runOnUiThread {
                            vDebugText.setText(data)
                        }

                    }

                })
            }
        }
    }

    fun updateView() {
        if (OBDManager.getInstance(applicationContext).isConnected) {
            vSerialPortStatus.text = "串口连接ok"
        } else {
            vSerialPortStatus.text = "串口连接失败"
        }
        if (NetManager.getInstance(applicationContext).isStarted) {
            vUploadStatus.text = "上传服务器连接ok"
        } else {
            vUploadStatus.text = "上传服务器连接失败"
        }
        if (NtripManager.getInstance().isNetworkIsConnected) {
            vNtripStatus.text = "ntrip连接ok"
        } else {
            vNtripStatus.text = "ntrip连接失败"
        }
    }

    private fun test() {
        testBtn.setOnClickListener {
            startActivity(Intent(this, TestLocActivity::class.java))
        }
        //test  获取电量
//        val kk=3/0

        val manager = getSystemService(BATTERY_SERVICE) as BatteryManager;
        val value1 = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        val value2 = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
        val value3 = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        val value4 = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);///当前电量百分比
        Log.d("xuxux", "test: ${value1}--${value2}--${value3}--${value4}")
        // manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS);///充电状态
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        test()
        rxPermissions.request(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).subscribe { grant ->
            if (grant) {
                createService()
            } else {
                finish()
            }
        }
        loadStorage()
        initView()
    }

    private fun createService() {
        val isDeamon = Storage.getIsDaemon(this)
        val intent = Intent(this@MainActivity, WorkService::class.java)
//        if (isDeamon) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
//            demonServiceStart=true
//        }
        bindService(intent, conn, Context.BIND_AUTO_CREATE)
    }

    private fun destroyService()
    {
        unbindService(conn)
        val isDeamon = Storage.getIsDaemon(this)
        if(!isDeamon)
        {
            stopService(Intent(this@MainActivity, WorkService::class.java))
        }
    }
    private fun initView() {

        vSaveLoc.setOnCheckedChangeListener { buttonView, isChecked ->
            service?.setRecorderLoc(isChecked)
        }
        vCheckService.setOnCheckedChangeListener { buttonView, isChecked ->

                Storage.saveIsDaemon(this@MainActivity,isChecked)

        }
        vGGa.setOnCheckedChangeListener { buttonView, isChecked ->

               service?.apply {
                   switchUploadGpgga(isChecked)
               }

        }
        vMockData.setOnCheckedChangeListener { buttonView, isChecked ->
            service?.apply {
                setMockUpload(isChecked)
            }

        }
        vSaveLoc.setText("信息存储${FileLogUtils.dirLoc}")
        vConnectNtrip.setOnClickListener {
            val server = vNtripServer.text.toString()
            val port = vPort.text.toString()
            val mount = vMount.text.toString()
            val usrName = vUserName.text.toString()
            val pass = vPassword.text.toString()
            if (checkData(server, "填写服务器地址") &&
                checkData(port, "请服务器端口") &&
                checkData(mount, "请服务器挂载点")
            ) {
                service?.apply {
                    Storage.saveNtripData(
                        this@MainActivity,
                        server,
                        port.toInt(),
                        mount,
                        usrName,
                        pass
                    )
                    val config = ConfigBean(server, port.toInt(), mount, usrName, pass, "", 0)
                    setNtipConfigData(config,vGGa.isChecked)

                }
            }

        }
        vConnectUpload.setOnClickListener {
            val uploadServer = vUpload.text.toString()
            val uploadServerport = vUploadPort.text.toString()
            val frequence = vFrequence.text.toString()

            val server = vNtripServer.text.toString()
            val port = vPort.text.toString()

            if (checkData(uploadServer, "上传服务器不能为空") && checkData(frequence, "上传频率不能为空")) {
                val config = ConfigBean(
                    "",
                    0,
                    "",
                    "",
                    "",
                    uploadServer,
                    uploadServerport.toInt(),
                    frequence.toInt()
                )
                //需求 如果和ntrip服务器一样 就复用ntrip的连接
                if(uploadServer == server && uploadServerport == port)
                {
                    service?.apply {
                        Storage.saveUploadData(
                            this@MainActivity,
                            uploadServer,
                            uploadServerport.toInt(),
                            frequence.toInt()
                        )
                        reuseNtripChanel(config)
                    }
                }else
                {
                    service?.apply {
                        Storage.saveUploadData(
                            this@MainActivity,
                            uploadServer,
                            uploadServerport.toInt(),
                            frequence.toInt()
                        )

                        setUploadConfigData(config)
                    }
                }
            }
        }
    }

    fun checkData(data: String, tip: String): Boolean {
        if (TextUtils.isEmpty(data)) {
            Toast.makeText(this, tip, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun loadStorage() {
        Storage.getData(this).apply {
            vNtripServer.setText(ntripServer)
            vPort.setText("${ntripServerPort}")
            vMount.setText(ntripServerMount)
            vUserName.setText(userName)
            vPassword.setText(password)
            vUpload.setText(uploadServer)
            vFrequence.setText("${uploadTime}")
            vUploadPort.setText("${uploadPort}")
        }
        vCheckService.isChecked = Storage.getIsDaemon(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyService()
    }
}