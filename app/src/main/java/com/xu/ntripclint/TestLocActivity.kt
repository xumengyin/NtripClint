package com.xu.ntripclint

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import com.xu.ntripclint.utils.LocManager
import com.xu.ntripclint.utils.Logs
import kotlinx.android.synthetic.main.activity_test_loc.*

class TestLocActivity : AppCompatActivity() {

    val listener1=object :LocManager.LocChangeLisener
    {
        @SuppressLint("SetTextI18n")
        override fun onLocationChanged(amapLocation: Location?) {
            amapLocation?.apply {
                locText1.text="${latitude}::${longitude}"
            }

        }
    }
    val listener2=object :LocManager.LocChangeNmeaLisener
    {
        override fun onLocationChanged(nmea: String?, time: Long) {
            if (nmea!!.contains("GPGGA")) {
                val result = nmea.split(",".toRegex()).toTypedArray()
                if (result.size >= 11) {
                    var gpsText=""
                    try {

                        if (!TextUtils.isEmpty(result[2]) && !TextUtils.isEmpty(result[4])) {
                            val lat = result[2].substring(0, 2).toDouble() + result[2]
                                    .substring(2).toDouble() / 60
                            val lng =
                                result[4].substring(0, 3).toDouble() + result[4]
                                    .substring(3).toDouble() / 60
                            Logs.w("解析Gpgga经纬度:$lat::$lng")
                            gpsText="解析Gpgga经纬度:$lat::$lng"
                        }
                        Logs.w("解析Gpgga-----" + result[6].toInt())
                        gpsText+=result[6]+"\n"+nmea
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    locText2.text=gpsText
                }
            }
        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_loc)


        LocManager.getInstance(this).addListener(listener1)
        LocManager.getInstance(this).addListener(listener2)

    }

    override fun onDestroy() {
        super.onDestroy()
        LocManager.getInstance(this).removeListener(listener1)
        LocManager.getInstance(this).removeListener(listener2)
    }
}