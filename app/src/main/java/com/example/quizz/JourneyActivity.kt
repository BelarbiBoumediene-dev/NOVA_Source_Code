package com.example.quizz

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.quizz.data.model.Chapter
import com.example.quizz.data.remote.JsonParser
import com.example.quizz.data.remote.toJourney

class JourneyActivity : AppCompatActivity() {

    private lateinit var journeyTitle: TextView
    private lateinit var journeyDescription: TextView
    private lateinit var goalText: TextView
    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var chaptersContainer: LinearLayout

    private lateinit var companionLevelText: TextView
    private lateinit var xpText: TextView
    private lateinit var companionAvatar: ImageView

    private var journeyId: String? = null
    private var journeyJson: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_journey)
        CosmeticThemeManager.apply(this)

        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0


        journeyTitle = findViewById(R.id.journeyTitle)
        journeyDescription = findViewById(R.id.journeyDescription)
        goalText = findViewById(R.id.goalText)
        progressText = findViewById(R.id.progressText)
        progressBar = findViewById(R.id.progressBar)
        xpText = findViewById(R.id.xpText)
        companionAvatar = findViewById(R.id.companionAvatar)

        companionLevelText =
            findViewById(R.id.companionLevelText)

        chaptersContainer = findViewById(R.id.chaptersContainer)


        journeyId = intent.getStringExtra("JOURNEY_ID")

        if (journeyId.isNullOrEmpty()) {
            finish()
            return
        }




        val companionAvatar =
            findViewById<ImageView>(R.id.companionAvatar)

        companionAvatar.setImageResource(
            CosmeticThemeManager.resultImageForScore(
                50,
                CosmeticThemeManager.selectedCosmetic(this)
            )
        )





        loadJourney()
    }

    private fun loadJourney() {

        try {

            val currentJourneyId =
                journeyId
                    ?: throw Exception("Journey ID is missing")

            val savedJourney =
                JourneyStorage.getJourney(
                    this,
                    currentJourneyId
                )

            if (savedJourney == null) {
                throw Exception("Journey not found")
            }

            journeyJson =
                savedJourney.journeyJson

            val response =
                JsonParser.parseJourney(
                    journeyJson!!
                )

            val journey =
                response.toJourney()

            journeyTitle.text =
                journey.title

            journeyDescription.text =
                journey.description

            goalText.text =
                journey.goal

            displayChapters(
                journey.chapters
            )

            updateProgress()

        } catch (e: Exception) {

            e.printStackTrace()

            journeyTitle.text =
                "Unable to load journey"

            journeyDescription.text =
                "Something went wrong while loading your journey."

            goalText.text =
                ""

            progressText.text =
                "0% Complete"

            progressBar.progress =
                0
        }
    }

    private fun displayChapters(
        chapters: List<Chapter>
    ) {

        chaptersContainer.removeAllViews()

        val currentJourneyId =
            journeyId ?: return

        chapters.forEachIndexed { index, chapter ->

            val previousChapterCompleted =
                if (index == 0) {

                    true

                } else {

                    ProgressManager.isChapterCompleted(
                        this,
                        currentJourneyId,
                        chapters[index - 1].id
                    )
                }

            val isCompleted =
                ProgressManager.isChapterCompleted(
                    this,
                    currentJourneyId,
                    chapter.id
                )

            val isUnlocked =
                index == 0 ||
                        previousChapterCompleted

            val updatedChapter =
                chapter.copy(
                    isLocked = !isUnlocked
                )

            val card =
                createChapterCard(
                    updatedChapter,
                    isCompleted
                )

            chaptersContainer.addView(
                card
            )

            if (index < chapters.lastIndex) {
                chaptersContainer.addView(createPathConnector())
            }
        }
    }

    private fun createPathConnector(): View {
        return View(this).apply {

            setBackgroundColor(
                Color.parseColor("#4F8CFF")
            )

            layoutParams =
                LinearLayout.LayoutParams(
                    2.dp(),
                    30.dp()
                ).apply {
                    gravity = android.view.Gravity.CENTER_HORIZONTAL
                }

            alpha = 0.45f
        }
    }

    private fun createChapterCard(
        chapter: Chapter,
        isCompleted: Boolean
    ): LinearLayout {

        val card = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                20.dp(),
                19.dp(),
                20.dp(),
                19.dp()
            )

            val background = GradientDrawable()

            background.setColor(
                Color.parseColor(
                    if (chapter.isLocked) {
                        "#0D1322"
                    } else {
                        "#101629"
                    }
                )
            )

            background.setStroke(
                1.dp(),
                Color.parseColor(
                    when {
                        isCompleted -> "#8B5CF6"
                        chapter.isLocked -> "#1D2940"
                        else -> "#283452"
                    }
                )
            )

            background.cornerRadius =
                20.dp().toFloat()

            this.background = background

            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {

                    setMargins(
                        0,
                        0,
                        0,
                        12.dp()
                    )
                }
        }

        // =========================================================
        // STAGE LABEL
        // =========================================================

        val chapterNumber =
            TextView(this).apply {

                text =
                    "◆  STAGE ${chapter.order}"

                textSize = 10f

                letterSpacing = 0.12f

                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )

                setTextColor(
                    Color.parseColor(
                        when {
                            isCompleted -> "#A78BFA"
                            chapter.isLocked -> "#4B5873"
                            else -> "#6EA8FF"
                        }
                    )
                )
            }

        card.addView(
            chapterNumber
        )

        // =========================================================
        // TITLE
        // =========================================================

        val title =
            TextView(this).apply {

                text =
                    chapter.title

                textSize = 20f

                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )

                setTextColor(
                    Color.parseColor(
                        if (chapter.isLocked) {
                            "#66718B"
                        } else {
                            "#F5F7FF"
                        }
                    )
                )

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {

                        setMargins(
                            0,
                            9.dp(),
                            0,
                            0
                        )
                    }
            }

        card.addView(
            title
        )

        // =========================================================
        // DESCRIPTION
        // =========================================================

        val description =
            TextView(this).apply {

                text =
                    chapter.description

                textSize = 13f

                setTextColor(
                    Color.parseColor(
                        if (chapter.isLocked) {
                            "#4F5A72"
                        } else {
                            "#78839F"
                        }
                    )
                )

                setLineSpacing(
                    3.dp().toFloat(),
                    1f
                )

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {

                        setMargins(
                            0,
                            7.dp(),
                            0,
                            0
                        )
                    }
            }

        card.addView(
            description
        )

        // =========================================================
        // STATUS
        // =========================================================

        val status =
            TextView(this).apply {

                text =
                    when {

                        isCompleted ->
                            "✓  COMPLETED"

                        chapter.isLocked ->
                            "🔒  LOCKED"

                        else ->
                            "▶  CHAPTER UNLOCKED"
                    }

                textSize = 10f

                letterSpacing = 0.08f

                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )

                setTextColor(
                    Color.parseColor(
                        when {

                            isCompleted ->
                                "#A78BFA"

                            chapter.isLocked ->
                                "#4F5A72"

                            else ->
                                "#6EA8FF"
                        }
                    )
                )

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {

                        setMargins(
                            0,
                            17.dp(),
                            0,
                            0
                        )
                    }
            }

        card.addView(
            status
        )

        // =========================================================
        // CLICK
        // =========================================================

        if (!chapter.isLocked) {

            card.isClickable = true
            card.isFocusable = true

            card.setOnClickListener {

                val intent =
                    Intent(
                        this,
                        ChapterActivity::class.java
                    )

                intent.putExtra(
                    "JOURNEY_ID",
                    journeyId
                )

                intent.putExtra(
                    "CHAPTER_ID",
                    chapter.id
                )

                startActivity(intent)
            }
        }

        return card
    }

    private fun updateProgress() {

        val currentJourneyId =
            journeyId ?: return

        val savedJourney =
            JourneyStorage.getJourney(
                this,
                currentJourneyId
            )

        if (savedJourney == null) {
            return
        }

        try {

            val response =
                JsonParser.parseJourney(
                    savedJourney.journeyJson
                )

            val journey =
                response.toJourney()

            val chapters =
                journey.chapters

            val totalChapters =
                chapters.size

            val completedChapters =
                chapters.count { chapter ->

                    ProgressManager.isChapterCompleted(
                        this,
                        currentJourneyId,
                        chapter.id
                    )
                }

            val journeyProgress =
                if (totalChapters > 0) {
                    (completedChapters * 100) /
                            totalChapters
                } else {
                    0
                }

            progressText.text =
                "$completedChapters / " +
                        "$totalChapters Chapters completed  •  " +
                        "$journeyProgress%"

            progressBar.max =
                100

            progressBar.progress =
                journeyProgress

            val xp =
                ProgressManager.getXp(
                    this,
                    currentJourneyId
                )

            val level =
                (xp / 100) + 1

            xpText.text =
                "⚡ $xp XP  •  Level $level"

            companionLevelText.text =
                "Level $level • Ready for adventure"




         } catch (e: Exception) {

        e.printStackTrace()

        progressText.text =
            "0 / 0 Chapters completed  •  0%"

        progressBar.max =
            100

        progressBar.progress =
            0

        xpText.text =
            "⚡ 0 XP  •  Level 1"

        companionLevelText.text =
            "Level 1 • Ready for adventure"
    }
    }

    override fun onResume() {

        super.onResume()

        if (::xpText.isInitialized) {
            updateProgress()
        }

        if (::chaptersContainer.isInitialized) {
            loadJourney()
        }
    }



    private fun Int.dp(): Int {
        return (
                this *
                        resources.displayMetrics.density
                ).toInt()
    }
}
