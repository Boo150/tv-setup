package com.youssef.animedextv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity

class SplashActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<TextView>(R.id.splash_logo)
        val tagline = findViewById<TextView>(R.id.splash_tagline)

        logo.alpha = 0f
        logo.animate().alpha(1f).setDuration(1000).start()
        tagline.alpha = 0f
        tagline.animate().alpha(1f).setDuration(1000).setStartDelay(500).start()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2200)
    }
}

