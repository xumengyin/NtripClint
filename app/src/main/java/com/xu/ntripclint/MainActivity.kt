package com.xu.ntripclint

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.tbruyelle.rxpermissions3.RxPermissions
import io.reactivex.rxjava3.functions.Consumer
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val rxPermissions = RxPermissions(this@MainActivity)
    var service: IBinder? = null
    val conn = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {

        }

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            binder?.apply {
                service = this
            }
        }
    }

    private fun test()
    {
        testBtn.setOnClickListener {
            startActivity(Intent(this,TestLocActivity::class.java))
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        test()
        rxPermissions.request(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).subscribe { grant ->
            if (grant) {
                val intent = Intent(this@MainActivity, WorkService::class.java)
                bindService(intent, conn, Context.BIND_AUTO_CREATE)
            } else {
                finish()
            }
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(conn)
    }
}