package com.example.quizz

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class MainActivity0 : AppCompatActivity() {

    private lateinit var gameTitle: TextView
    private lateinit var loadingText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var videoView: VideoView

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main0)

        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0

        gameTitle = findViewById(R.id.gameTitle)
        loadingText = findViewById(R.id.loadingText)
        progressBar = findViewById(R.id.progressBar)
        videoView = findViewById(R.id.videoBackground)

        setupVideo()

        gameTitle.setShadowLayer(
            25f,
            0f,
            0f,
            Color.parseColor("#66CCFF")
        )

        startIntroAnimation()
    }

    private fun setupVideo() {

        val videoUri = Uri.parse(
            "android.resource://$packageName/${R.raw.background_video1}"
        )

        videoView.setVideoURI(videoUri)

        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true
            mp.setVolume(0f, 0f)
            videoView.start()
        }
    }

    private fun startIntroAnimation() {

        gameTitle.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(900)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        gameTitle.postDelayed({

            loadingText.animate()
                .alpha(1f)
                .setDuration(500)
                .start()

            progressBar.animate()
                .alpha(1f)
                .setDuration(500)
                .start()

        }, 550)

        val duration = 6000L
        val startTime = System.currentTimeMillis()

        val progressRunnable = object : Runnable {

            override fun run() {

                val elapsed =
                    System.currentTimeMillis() - startTime

                val progress =
                    ((elapsed.toFloat() / duration) * 100)
                        .toInt()
                        .coerceIn(0, 100)

                progressBar.progress = progress

                loadingText.text =
                    "LOADING $progress%"

                if (progress >= 100) {

                    progressBar.progress = 100
                    loadingText.text = "LOADING 100%"

                    startActivity(
                        Intent(
                            this@MainActivity0,
                            WelcomeActivity::class.java
                        )
                    )

                    finish()

                } else {

                    handler.postDelayed(this, 16)
                }
            }
        }

        handler.post(progressRunnable)
    }

    private fun openGame() {

        gameTitle.animate()
            .alpha(0f)
            .scaleX(1.12f)
            .scaleY(1.12f)
            .setDuration(350)
            .start()

        loadingText.animate()
            .alpha(0f)
            .setDuration(250)
            .start()

        progressBar.animate()
            .alpha(0f)
            .setDuration(250)
            .withEndAction {

                startActivity(
                    Intent(
                        this,
                        WelcomeActivity::class.java
                    )
                )

                overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )

                finish()
            }
            .start()
    }

    override fun onPause() {
        super.onPause()

        if (::videoView.isInitialized) {
            videoView.pause()
        }
    }

    override fun onResume() {
        super.onResume()

        if (::videoView.isInitialized) {
            videoView.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        handler.removeCallbacksAndMessages(null)

        if (::videoView.isInitialized) {
            videoView.stopPlayback()
        }
    }
}