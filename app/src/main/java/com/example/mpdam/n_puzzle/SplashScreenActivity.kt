package com.example.mpdam.n_puzzle

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity


class SplashScreenActivity : AppCompatActivity() {

    private lateinit var logoAnimation: AnimationDrawable
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

       // initSplashScreen()
        launchApp()
    }
/*
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        animateSplashScreen()
    }
    private fun initSplashScreen() {
        val ivLogo = findViewById<ImageView>(R.id.iv_splash_logo)
        ivLogo.setBackgroundResource(R.drawable.logo_animation)
        logoAnimation = ivLogo.background as AnimationDrawable
    }
    private fun animateSplashScreen() {
        logoAnimation.setExitFadeDuration(AnimationUtil.ANIMATION_FRAME_FADEOUT)
        logoAnimation.start()
    }*/
    private fun launchApp() {
        Handler(Looper.getMainLooper()).postDelayed({
            val i = Intent(this@SplashScreenActivity, NPuzzleActivity::class.java)
            startActivity(i)
            finish()
        }, AnimationUtil.SPLASH_SCREEN_TIMEOUT.toLong())
    }
}