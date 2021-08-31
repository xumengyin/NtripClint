package com.jerry.ntripwatchdog

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class MainWatchActivity : AppCompatActivity() {
    val handler by lazy {
        Handler(Looper.getMainLooper())
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_watch)
        val intent = Intent(this@MainWatchActivity,WatchService::class.java)
        startService(intent)
        handler.postDelayed({
                finish()
        },2000)
    }
}