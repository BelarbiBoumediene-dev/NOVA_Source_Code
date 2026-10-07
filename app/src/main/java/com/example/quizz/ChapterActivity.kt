package com.example.quizz

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.quizz.data.model.Chapter
import com.example.quizz.data.remote.JsonParser
import com.example.quizz.data.remote.toJourney

class ChapterActivity : AppCompatActivity() {

    private lateinit var chapterTitle: TextView
    private lateinit var chapterDescription: TextView
    private lateinit var lessonContainer: LinearLayout
    private lateinit var challengeText: TextView

    private var journeyId: String? = null
    private var chapterId: String? = null

    // =========================================================
    // NOVA COLORS
    // =========================================================

    private val novaBlue = Color.parseColor("#4F8CFF")
    private val novaBlueLight = Color.parseColor("#6EA8FF")

    private val novaPurple = Color.parseColor("#8B5CF6")
    private val novaPurpleLight = Color.parseColor("#A78BFA")

    private val cardColor = Color.parseColor("#101629")
    private val cardColorDeep = Color.parseColor("#0D1427")

    private val borderColor = Color.parseColor("#283452")

    private val textPrimary = Color.parseColor("#F5F7FF")
    private val textSecondary = Color.parseColor("#78839F")
    private val textMuted = Color.parseColor("#626D89")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_chapter)
        CosmeticThemeManager.apply(this)

        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0

        bindViews()
        readIntentData()

        if (
            journeyId.isNullOrEmpty() ||
            chapterId.isNullOrEmpty()
        ) {
            finish()
            return
        }

        loadChapter()
    }

    // =========================================================
    // VIEW BINDING
    // =========================================================

    private fun bindViews() {

        chapterTitle =
            findViewById(R.id.chapterTitle)

        chapterDescription =
            findViewById(R.id.chapterDescription)

        lessonContainer =
            findViewById(R.id.lessonContainer)

        challengeText =
            findViewById(R.id.challengeText)
    }

    // =========================================================
    // INTENT DATA
    // =========================================================

    private fun readIntentData() {

        journeyId =
            intent.getStringExtra("JOURNEY_ID")

        chapterId =
            intent.getStringExtra("CHAPTER_ID")
    }

    // =========================================================
    // LOAD CHAPTER
    // =========================================================

    private fun loadChapter() {

        try {

            val currentJourneyId =
                journeyId
                    ?: throw Exception("Journey ID is missing")

            val currentChapterId =
                chapterId
                    ?: throw Exception("Chapter ID is missing")

            val savedJourney =
                JourneyStorage.getJourney(
                    this,
                    currentJourneyId
                )
                    ?: throw Exception("Journey not found")

            val response =
                JsonParser.parseJourney(
                    savedJourney.journeyJson
                )

            val journey =
                response.toJourney()

            val chapterIndex =
                journey.chapters.indexOfFirst {
                    it.id == currentChapterId
                }

            if (chapterIndex == -1) {
                finish()
                return
            }

            // =================================================
            // CHAPTER UNLOCK SYSTEM
            // =================================================

            if (chapterIndex > 0) {

                val previousChapter =
                    journey.chapters[chapterIndex - 1]

                val previousCompleted =
                    ProgressManager.isChapterCompleted(
                        this,
                        currentJourneyId,
                        previousChapter.id
                    )

                if (!previousCompleted) {

                    Toast.makeText(
                        this,
                        "Complete the previous chapter first.",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                    return
                }
            }

            val chapter =
                journey.chapters[chapterIndex]

            displayChapter(chapter)

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Unable to load chapter.",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }

    // =========================================================
    // DISPLAY CHAPTER
    // =========================================================

    private fun displayChapter(
        chapter: Chapter
    ) {

        chapterTitle.text =
            chapter.title

        chapterDescription.text =
            chapter.description

        displayLessons(chapter)
        displayChallenge(chapter)
    }

    // =========================================================
    // LESSONS
    // =========================================================

    private fun displayLessons(
        chapter: Chapter
    ) {

        lessonContainer.removeAllViews()

        val currentJourneyId =
            journeyId ?: return

        chapter.lessons.forEachIndexed { index, lesson ->

            val completed =
                ProgressManager.isLessonCompleted(
                    this,
                    currentJourneyId,
                    lesson.id
                )

            val card =
                createLessonCard(
                    index = index,
                    lessonTitle = lesson.title,
                    estimatedMinutes = lesson.estimatedMinutes,
                    quizCount = lesson.quiz.questions.size,
                    completed = completed
                )

            card.setOnClickListener {

                val intent =
                    Intent(
                        this,
                        LessonActivity::class.java
                    )

                intent.putExtra(
                    "JOURNEY_ID",
                    journeyId
                )

                intent.putExtra(
                    "CHAPTER_ID",
                    chapter.id
                )

                intent.putExtra(
                    "LESSON_ID",
                    lesson.id
                )

                startActivity(intent)
            }

            lessonContainer.addView(card)
        }
    }

    // =========================================================
    // CREATE LESSON CARD
    // =========================================================

    private fun createLessonCard(
        index: Int,
        lessonTitle: String,
        estimatedMinutes: Int,
        quizCount: Int,
        completed: Boolean
    ): LinearLayout {

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    18.dp(),
                    18.dp(),
                    18.dp(),
                    18.dp()
                )

                isClickable = true
                isFocusable = true

                background =
                    createLessonBackground(
                        completed
                    )

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

        // =====================================================
        // TOP ROW
        // =====================================================

        val topRow =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    android.view.Gravity.CENTER_VERTICAL
            }

        // =====================================================
        // QUEST ICON
        // =====================================================

        val icon =
            TextView(this).apply {

                text =
                    if (completed) "✓"
                    else "⚔"

                gravity =
                    android.view.Gravity.CENTER

                textSize =
                    18f

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setTextColor(
                    if (completed)
                        novaPurpleLight
                    else
                        novaBlueLight
                )

                background =
                    createIconBackground(
                        completed
                    )

                layoutParams =
                    LinearLayout.LayoutParams(
                        46.dp(),
                        46.dp()
                    )
            }

        topRow.addView(icon)

        // =====================================================
        // TITLE AREA
        // =====================================================

        val titleContainer =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {

                        setMargins(
                            14.dp(),
                            0,
                            10.dp(),
                            0
                        )
                    }
            }

        val lessonNumber =
            TextView(this).apply {

                text =
                    if (completed)
                        "◆  QUEST ${String.format("%02d", index + 1)}"
                    else
                        "◆  QUEST ${String.format("%02d", index + 1)}"

                textSize =
                    8f

                letterSpacing =
                    0.14f

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setTextColor(
                    if (completed)
                        novaPurpleLight
                    else
                        novaBlueLight
                )
            }

        titleContainer.addView(
            lessonNumber
        )

        val title =
            TextView(this).apply {

                text =
                    lessonTitle

                textSize =
                    17f

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setTextColor(
                    textPrimary
                )

                maxLines = 2

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {

                        setMargins(
                            0,
                            4.dp(),
                            0,
                            0
                        )
                    }
            }

        titleContainer.addView(
            title
        )

        topRow.addView(
            titleContainer
        )

        // =====================================================
        // ARROW
        // =====================================================

        val arrow =
            TextView(this).apply {

                text =
                    if (completed) "✓"
                    else "›"

                gravity =
                    android.view.Gravity.CENTER

                textSize =
                    if (completed) 15f else 28f

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setTextColor(
                    if (completed)
                        novaPurpleLight
                    else
                        novaBlueLight
                )

                layoutParams =
                    LinearLayout.LayoutParams(
                        32.dp(),
                        42.dp()
                    )
            }

        topRow.addView(arrow)

        card.addView(topRow)

        // =====================================================
        // DIVIDER
        // =====================================================

        val divider =
            android.view.View(this).apply {

                setBackgroundColor(
                    Color.parseColor("#202943")
                )

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1.dp()
                    ).apply {

                        setMargins(
                            0,
                            15.dp(),
                            0,
                            13.dp()
                        )
                    }
            }

        card.addView(divider)

        // =====================================================
        // INFO ROW
        // =====================================================

        val infoRow =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    android.view.Gravity.CENTER_VERTICAL
            }

        val time =
            createInfoText(
                "◷  ${estimatedMinutes} MIN"
            )

        val questions =
            createInfoText(
                "◆  $quizCount QUESTIONS"
            )

        infoRow.addView(time)

        infoRow.addView(
            questions.apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(
                            16.dp(),
                            0,
                            0,
                            0
                        )
                    }
            }
        )

        card.addView(infoRow)

        // =====================================================
        // STATUS
        // =====================================================

        val status =
            TextView(this).apply {

                text =
                    if (completed)
                        "✓  QUEST COMPLETED"
                    else
                        "▶  START QUEST"

                textSize =
                    9f

                letterSpacing =
                    0.10f

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setTextColor(
                    if (completed)
                        novaPurpleLight
                    else
                        novaBlueLight
                )

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {

                        setMargins(
                            0,
                            14.dp(),
                            0,
                            0
                        )
                    }
            }

        card.addView(status)

        return card
    }

    // =========================================================
    // INFO TEXT
    // =========================================================

    private fun createInfoText(
        text: String
    ): TextView {

        return TextView(this).apply {

            this.text =
                text

            textSize =
                9f

            letterSpacing =
                0.06f

            setTypeface(
                null,
                Typeface.BOLD
            )

            setTextColor(
                textMuted
            )
        }
    }

    // =========================================================
    // LESSON BACKGROUND
    // =========================================================

    private fun createLessonBackground(
        completed: Boolean
    ): GradientDrawable {

        return GradientDrawable().apply {

            shape =
                GradientDrawable.RECTANGLE

            cornerRadius =
                20.dp().toFloat()

            setColor(
                cardColor
            )

            setStroke(
                1.dp(),
                if (completed)
                    novaPurple
                else
                    borderColor
            )
        }
    }

    // =========================================================
    // LESSON ICON BACKGROUND
    // =========================================================

    private fun createIconBackground(
        completed: Boolean
    ): GradientDrawable {

        return GradientDrawable().apply {

            shape =
                GradientDrawable.RECTANGLE

            cornerRadius =
                14.dp().toFloat()

            setColor(
                if (completed)
                    Color.parseColor("#19132F")
                else
                    Color.parseColor("#101D35")
            )

            setStroke(
                1.dp(),
                if (completed)
                    Color.parseColor("#6040A8")
                else
                    Color.parseColor("#294A83")
            )
        }
    }

    // =========================================================
    // FINAL CHALLENGE
    // =========================================================

    private fun displayChallenge(
        chapter: Chapter
    ) {

        val currentJourneyId =
            journeyId ?: return

        val challenge =
            chapter.challenge

        if (challenge == null) {

            challengeText.text =
                "⚔  NO BOSS CHALLENGE AVAILABLE"

            challengeText.setTextColor(
                textMuted
            )

            challengeText.setOnClickListener(null)

            return
        }

        val completed =
            ProgressManager.isChallengeCompleted(
                this,
                currentJourneyId,
                challenge.id
            )

        // =====================================================
        // BACKGROUND
        // =====================================================

        challengeText.background =
            GradientDrawable().apply {

                shape =
                    GradientDrawable.RECTANGLE

                cornerRadius =
                    20.dp().toFloat()

                setColor(
                    if (completed)
                        Color.parseColor("#15112A")
                    else
                        Color.parseColor("#11182C")
                )

                setStroke(
                    1.dp(),
                    if (completed)
                        novaPurple
                    else
                        novaBlue
                )
            }

        challengeText.setPadding(
            20.dp(),
            19.dp(),
            20.dp(),
            19.dp()
        )

        // =====================================================
        // TEXT
        // =====================================================

        challengeText.text =
            if (completed) {

                "✓  ${challenge.title}\n" +
                        "BOSS DEFEATED  •  CHAPTER COMPLETED"

            } else {

                "⚔  ${challenge.title}\n" +
                        "FINAL BOSS  •  COMPLETE THE CHAPTER"
            }

        challengeText.setTextColor(
            if (completed)
                novaPurpleLight
            else
                textPrimary
        )

        challengeText.textSize =
            14f

        challengeText.setTypeface(
            null,
            Typeface.BOLD
        )

        // =====================================================
        // CLICK
        // =====================================================

        if (completed) {

            challengeText.setOnClickListener(null)

            challengeText.isClickable =
                false

        } else {

            challengeText.isClickable =
                true

            challengeText.isFocusable =
                true

            challengeText.setOnClickListener {

                if (
                    ProgressManager.isChallengeCompleted(
                        this,
                        currentJourneyId,
                        challenge.id
                    )
                ) {
                    return@setOnClickListener
                }

                val intent =
                    Intent(
                        this,
                        ChallengeActivity::class.java
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
    }

    // =========================================================
    // RESUME
    // =========================================================

    override fun onResume() {
        super.onResume()

        if (
            !journeyId.isNullOrEmpty() &&
            !chapterId.isNullOrEmpty()
        ) {
            loadChapter()
        }
    }

    // =========================================================
    // DP
    // =========================================================

    private fun Int.dp(): Int {
        return (
                this *
                        resources.displayMetrics.density
                ).toInt()
    }
}
