package com.example.quizz

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.quizz.data.remote.JourneyRepository
import com.example.quizz.data.remote.JourneyRequestException
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException

class GeneratingActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var progressText: TextView
    private lateinit var hintText: TextView
    private lateinit var retryButton: com.google.android.material.button.MaterialButton

    private val repository = JourneyRepository()

    private val handler = Handler(Looper.getMainLooper())

    private var goal = ""
    private var level = ""
    private var dailyTime = ""
    private var duration = ""
    private var isGenerating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0

        setContentView(R.layout.activity_generating)
        CosmeticThemeManager.apply(this)

        // =====================================================
        // VIEWS
        // =====================================================

        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        progressText = findViewById(R.id.progressText)
        hintText = findViewById(R.id.hintText)
        retryButton = findViewById(R.id.retryButton)
        retryButton.setOnClickListener { startGeneration() }

        // =====================================================
        // INTENT DATA
        // =====================================================

        goal = intent.getStringExtra("GOAL") ?: ""
        level = intent.getStringExtra("LEVEL") ?: ""
        dailyTime = intent.getStringExtra("DAILY_TIME") ?: ""
        duration = intent.getStringExtra("DURATION") ?: ""


        val companionAvatar =
            findViewById<ImageView>(R.id.companionAvatar)

        companionAvatar.setImageResource(
            CosmeticThemeManager.resultImageForScore(
                50,
                CosmeticThemeManager.selectedCosmetic(this)
            )
        )




        // =====================================================
        // START
        // =====================================================

        startGeneration()
    }

    private fun startGeneration() {

        if (isGenerating) return

        if (!NetworkMonitor.isOnline(this)) {
            showFailure(
                "You're offline",
                "Connect to the internet, then retry to create your journey."
            )
            return
        }

        isGenerating = true
        handler.removeCallbacksAndMessages(null)
        retryButton.visibility = android.view.View.GONE

        progressBar.max = 100

        updateProgress(
            progress = 10,
            status = "Understanding your goal...",
            hint = "Analyzing your objective and preparing your adventure."
        )

        handler.postDelayed({

            updateProgress(
                progress = 25,
                status = "Connecting to AI...",
                hint = "Nova is designing your personalized world."
            )

        }, 700)

        handler.postDelayed({

            updateProgress(
                progress = 40,
                status = "Building your journey...",
                hint = "Creating chapters, lessons and quests."
            )

        }, 1400)

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val journey =
                    repository.generateJourney(
                        goal = goal,
                        level = level,
                        dailyTime = dailyTime,
                        duration = duration
                    )

                withContext(Dispatchers.Main) {

                    updateProgress(
                        progress = 80,
                        status = "Preparing your challenges...",
                        hint = "Your quests are almost ready."
                    )

                    handler.postDelayed({

                        updateProgress(
                            progress = 100,
                            status = "Your world is ready!",
                            hint = "Your adventure awaits. Enter your new world."
                        )

                        openJourney(journey)

                    }, 700)
                }

            } catch (e: Exception) {

                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    val (title, message) = when (e) {
                        is JourneyRequestException -> when (e.statusCode) {
                            400, 401, 403 -> "Request needs attention" to
                                "Check your quest details and try again."
                            429 -> "Nova is busy" to
                                "Please wait a moment before trying again."
                            in 500..599 -> "Nova is temporarily unavailable" to
                                "Our server is having trouble. Please retry shortly."
                            else -> "Generation failed" to
                                "We couldn't create your journey. Please try again."
                        }
                        is SocketTimeoutException -> "Connection timed out" to
                            "The server took too long to respond. Please try again."
                        is IOException -> "Connection lost" to
                            "Check your internet connection, then retry."
                        else -> "Generation failed" to
                            "We couldn't create your journey. Please try again."
                    }
                    showFailure(title, message)

                }
            }
        }
    }

    // =========================================================
    // UPDATE UI
    // =========================================================

    private fun updateProgress(
        progress: Int,
        status: String,
        hint: String
    ) {

        progressBar.progress = progress

        statusText.text = status

        progressText.text = "$progress%"

        hintText.text = hint
    }

    private fun showFailure(status: String, hint: String) {
        isGenerating = false
        handler.removeCallbacksAndMessages(null)
        updateProgress(progress = 0, status = status, hint = hint)
        retryButton.visibility = View.VISIBLE
        retryButton.isEnabled = true
    }

    // =========================================================
    // OPEN JOURNEY
    // =========================================================

    private fun openJourney(
        journey: com.example.quizz.data.remote.JourneyResponse
    ) {

        /*
         * Convert AI response to JSON.
         */
        val journeyJson =
            Gson().toJson(journey)

        /*
         * Add this Journey to the
         * existing saved Journeys.
         *
         * Previous Journeys are NOT deleted.
         */
        val savedJourney =
            JourneyStorage.addJourney(
                context = this,
                journeyJson = journeyJson,
                title = journey.title,
                goal = journey.goal,
                level = journey.level,
                dailyTime = journey.dailyTime,
                duration = journey.duration
            )

        /*
         * Open the generated Journey.
         */
        val intent =
            Intent(
                this,
                JourneyActivity::class.java
            )

        intent.putExtra(
            "JOURNEY_ID",
            savedJourney.id
        )

        startActivity(intent)

        finish()
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    override fun onDestroy() {
        super.onDestroy()

        handler.removeCallbacksAndMessages(null)
    }
}
