package com.example.quizz

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.quizz.data.model.QuizQuestion
import com.example.quizz.data.remote.JsonParser
import com.example.quizz.data.remote.toJourney
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ChallengeActivity : AppCompatActivity() {

    private lateinit var challengeTitle: TextView
    private lateinit var challengeType: TextView
    private lateinit var challengeDescription: TextView

    private lateinit var questionCounter: TextView
    private lateinit var questionText: TextView
    private lateinit var optionsGroup: RadioGroup

    private lateinit var feedbackText: TextView
    private lateinit var completeButton: MaterialButton

    private var questions: List<QuizQuestion> =
        emptyList()

    private var currentQuestionIndex = 0
    private var score = 0
    private var answerChecked = false

    private var journeyId: String? = null
    private var chapterId: String? = null
    private var challengeId: String? = null

    private var passingScore = 70

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_challenge)
        CosmeticThemeManager.apply(this)

        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0

        // --------------------------------------------------
        // VIEWS
        // --------------------------------------------------

        challengeTitle =
            findViewById(R.id.challengeTitle)

        challengeType =
            findViewById(R.id.challengeType)

        challengeDescription =
            findViewById(R.id.challengeDescription)

        questionCounter =
            findViewById(R.id.questionCounter)

        questionText =
            findViewById(R.id.questionText)

        optionsGroup =
            findViewById(R.id.optionsGroup)

        feedbackText =
            findViewById(R.id.feedbackText)

        completeButton =
            findViewById(R.id.completeButton)

        // --------------------------------------------------
        // IDS
        // --------------------------------------------------

        journeyId =
            intent.getStringExtra("JOURNEY_ID")

        chapterId =
            intent.getStringExtra("CHAPTER_ID")

        if (
            journeyId.isNullOrEmpty() ||
            chapterId.isNullOrEmpty()
        ) {
            finish()
            return
        }

        // --------------------------------------------------
        // BUTTON
        // --------------------------------------------------

        completeButton.setOnClickListener {

            if (answerChecked) {
                goToNextQuestion()
            } else {
                checkAnswer()
            }
        }

        loadChallenge()
    }

    // ======================================================
    // LOAD CHALLENGE
    // ======================================================

    private fun loadChallenge() {

        try {

            val currentJourneyId =
                journeyId
                    ?: throw Exception(
                        "Journey ID is missing"
                    )

            val currentChapterId =
                chapterId
                    ?: throw Exception(
                        "Chapter ID is missing"
                    )

            val savedJourney =
                JourneyStorage.getJourney(
                    this,
                    currentJourneyId
                )

            if (savedJourney == null) {
                throw Exception(
                    "Journey not found"
                )
            }

            val response =
                JsonParser.parseJourney(
                    savedJourney.journeyJson
                )

            val journey =
                response.toJourney()

            val chapter =
                journey.chapters.find {
                    it.id == currentChapterId
                }

            if (chapter == null) {
                throw Exception(
                    "Chapter not found"
                )
            }

            val challenge =
                chapter.challenge
                    ?: throw Exception(
                        "Challenge not found"
                    )

            challengeId =
                challenge.id

            passingScore =
                challenge.passingScore

            challengeTitle.text =
                challenge.title

            challengeType.text =
                "${challenge.type.uppercase()} CHALLENGE"

            challengeDescription.text =
                challenge.description

            questions =
                challenge.questions

            if (questions.isEmpty()) {
                throw Exception(
                    "No challenge questions available"
                )
            }

            // --------------------------------------------------
            // ALREADY COMPLETED
            // --------------------------------------------------

            if (
                ProgressManager.isChallengeCompleted(
                    this,
                    currentJourneyId,
                    challenge.id
                )
            ) {

                feedbackText.visibility =
                    View.VISIBLE

                feedbackText.text =
                    "✓ Challenge already completed."

                completeButton.text =
                    "COMPLETED"

                completeButton.isEnabled =
                    false

                return
            }

            showQuestion()

        } catch (e: Exception) {

            e.printStackTrace()

            finish()
        }
    }

    // ======================================================
    // SHOW QUESTION
    // ======================================================

    private fun showQuestion() {

        if (questions.isEmpty()) {
            finish()
            return
        }

        val question =
            questions[currentQuestionIndex]

        val current =
            currentQuestionIndex + 1

        val total =
            questions.size

        // --------------------------------------------------
        // COUNTER
        // --------------------------------------------------

        questionCounter.text =
            "CHALLENGE QUESTION $current / $total"

        // --------------------------------------------------
        // QUESTION
        // --------------------------------------------------

        questionText.text =
            question.question

        // --------------------------------------------------
        // RESET OPTIONS
        // --------------------------------------------------

        optionsGroup.clearCheck()
        optionsGroup.removeAllViews()

        // --------------------------------------------------
        // CREATE LONG OPTIONS
        // --------------------------------------------------

        question.options.forEachIndexed {
                index,
                option ->

            val radioButton =
                RadioButton(this)

            radioButton.id =
                View.generateViewId()

            radioButton.tag =
                index

            radioButton.text =
                option

            radioButton.textSize =
                16f

            radioButton.setTextColor(
                Color.parseColor("#DCE2F2")
            )

            // --------------------------------------------------
            // RADIO BUTTON COLOR
            // --------------------------------------------------

            radioButton.buttonTintList =
                ColorStateList(
                    arrayOf(
                        intArrayOf(
                            android.R.attr.state_checked
                        ),
                        intArrayOf(
                            -android.R.attr.state_checked
                        )
                    ),
                    intArrayOf(
                        Color.parseColor("#6EA8FF"),
                        Color.parseColor("#626D89")
                    )
                )

            // --------------------------------------------------
            // LONG FULL-WIDTH OPTION
            // --------------------------------------------------

            val params =
                RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT,
                    72.dp()
                )

            params.setMargins(
                0,
                0,
                0,
                12.dp()
            )

            radioButton.layoutParams =
                params

            // --------------------------------------------------
            // INTERNAL PADDING
            // --------------------------------------------------

            radioButton.setPadding(
                20.dp(),
                0,
                20.dp(),
                0
            )

            radioButton.gravity =
                Gravity.CENTER_VERTICAL

            radioButton.minHeight =
                72.dp()

            radioButton.maxLines =
                3

            radioButton.includeFontPadding =
                true

            // --------------------------------------------------
            // DEFAULT BACKGROUND
            // --------------------------------------------------

            radioButton.background =
                createOptionBackground(
                    selected = false
                )

            // --------------------------------------------------
            // CLICK
            // --------------------------------------------------

            radioButton.setOnClickListener {

                if (!answerChecked) {
                    updateOptionStates()
                }
            }

            optionsGroup.addView(
                radioButton
            )
        }

        // --------------------------------------------------
        // RESET FEEDBACK
        // --------------------------------------------------

        feedbackText.visibility =
            View.GONE

        feedbackText.text =
            ""

        // --------------------------------------------------
        // RESET BUTTON
        // --------------------------------------------------

        completeButton.text =
            "CHECK ANSWER   →"

        completeButton.isEnabled =
            true

        answerChecked =
            false

        updateOptionStates()
    }

    // ======================================================
    // OPTION BACKGROUND
    // ======================================================

    private fun createOptionBackground(
        selected: Boolean
    ): GradientDrawable {

        return GradientDrawable().apply {

            shape =
                GradientDrawable.RECTANGLE

            cornerRadius =
                18.dp().toFloat()

            if (selected) {

                setColor(
                    Color.parseColor("#162544")
                )

                setStroke(
                    2.dp(),
                    Color.parseColor("#4F8CFF")
                )

            } else {

                setColor(
                    Color.parseColor("#101629")
                )

                setStroke(
                    1.dp(),
                    Color.parseColor("#283452")
                )
            }
        }
    }

    // ======================================================
    // UPDATE OPTION STATES
    // ======================================================

    private fun updateOptionStates() {

        for (i in 0 until optionsGroup.childCount) {

            val button =
                optionsGroup.getChildAt(i)
                        as? RadioButton
                    ?: continue

            val selected =
                button.isChecked

            button.background =
                createOptionBackground(
                    selected = selected
                )

            if (selected) {

                button.setTextColor(
                    Color.parseColor("#F5F7FF")
                )

            } else {

                button.setTextColor(
                    Color.parseColor("#DCE2F2")
                )
            }
        }
    }

    // ======================================================
    // CHECK ANSWER
    // ======================================================

    private fun checkAnswer() {

        val selectedId =
            optionsGroup.checkedRadioButtonId

        if (selectedId == -1) {

            feedbackText.visibility =
                View.VISIBLE

            feedbackText.text =
                "Please select an answer."

            return
        }

        val selectedButton =
            findViewById<RadioButton>(
                selectedId
            )

        val selectedIndex =
            selectedButton.tag as? Int
                ?: return

        val question =
            questions[currentQuestionIndex]

        val isCorrect =
            selectedIndex ==
                    question.correctAnswer

        if (isCorrect) {

            score++

            feedbackText.text =
                "✓ Correct!\n\n" +
                        question.explanation

        } else {

            feedbackText.text =
                "✗ Incorrect.\n\n" +
                        question.explanation
        }

        feedbackText.visibility =
            View.VISIBLE

        // --------------------------------------------------
        // LOCK OPTIONS
        // --------------------------------------------------

        for (i in 0 until optionsGroup.childCount) {

            val button =
                optionsGroup.getChildAt(i)

            button.isEnabled =
                false
        }

        answerChecked =
            true

        // --------------------------------------------------
        // BUTTON
        // --------------------------------------------------

        completeButton.text =
            if (
                currentQuestionIndex ==
                questions.lastIndex
            ) {
                "FINISH CHALLENGE   →"
            } else {
                "NEXT QUESTION   →"
            }
    }

    // ======================================================
    // NEXT QUESTION
    // ======================================================

    private fun goToNextQuestion() {

        if (
            currentQuestionIndex <
            questions.lastIndex
        ) {

            currentQuestionIndex++

            showQuestion()

        } else {

            finishChallenge()
        }
    }

    // ======================================================
    // FINISH CHALLENGE
    // ======================================================

    private fun finishChallenge() {

        // ======================================================
        // IDS
        // ======================================================

        val currentJourneyId =
            journeyId

        val currentChapterId =
            chapterId

        val currentChallengeId =
            challengeId

        if (
            currentJourneyId == null ||
            currentChapterId == null ||
            currentChallengeId == null
        ) {

            feedbackText.visibility =
                View.VISIBLE

            feedbackText.text =
                "Unable to identify this challenge."

            return
        }

        // ======================================================
        // CALCULATE SCORE
        // ======================================================

        val percentage =
            if (questions.isNotEmpty()) {

                (score * 100) /
                        questions.size

            } else {
                0
            }

        // ======================================================
        // CHALLENGE FAILED
        // ======================================================

        if (percentage < passingScore) {

            feedbackText.visibility =
                View.VISIBLE

            feedbackText.text =
                "Challenge not passed.\n\n" +
                        "Score: $percentage%\n" +
                        "Required: $passingScore%\n\n" +
                        "Review the chapter and try again."

            completeButton.text =
                "TRY AGAIN   ↻"

            completeButton.isEnabled =
                true

            completeButton.setOnClickListener {

                // Reset challenge
                currentQuestionIndex = 0
                score = 0
                answerChecked = false

                // Show first question again
                showQuestion()

                // Restore normal button behavior
                completeButton.setOnClickListener {

                    if (answerChecked) {

                        goToNextQuestion()

                    } else {

                        checkAnswer()
                    }
                }
            }

            return
        }

        // ======================================================
        // ALREADY COMPLETED CHECK
        // ======================================================

        if (
            ProgressManager.isChallengeCompleted(
                this,
                currentJourneyId,
                currentChallengeId
            )
        ) {

            feedbackText.visibility =
                View.VISIBLE

            feedbackText.text =
                "✓ Challenge already completed."

            completeButton.text =
                "COMPLETED"

            completeButton.isEnabled =
                false

            return
        }

        // ======================================================
        // LOCAL CHALLENGE COMPLETION
        // ======================================================

        ProgressManager.completeChallenge(
            this,
            currentJourneyId,
            currentChallengeId
        )

        // ======================================================
        // REWARDS
        // ======================================================

        val earnedXp =
            20

        val earnedCoins =
            5

        ProgressManager.addXp(
            this,
            currentJourneyId,
            earnedXp
        )

        // ======================================================
        // FIREBASE
        // ======================================================

        lifecycleScope.launch {

            // --------------------------------------------------
            // SAVE XP + COINS
            // --------------------------------------------------

            val xpResult =
                FirebaseProgressManager
                    .addToTotalProgress(
                        earnedXp = earnedXp,
                        earnedCoins = earnedCoins
                    )

            if (xpResult.isFailure) {

                xpResult
                    .exceptionOrNull()
                    ?.printStackTrace()
            }

            // --------------------------------------------------
            // SAVE CHALLENGE COMPLETION
            // --------------------------------------------------

            val challengeResult =
                FirebaseProgressManager
                    .saveChallengeCompletion(
                        journeyId =
                            currentJourneyId,
                        challengeId =
                            currentChallengeId
                    )

            if (challengeResult.isFailure) {

                challengeResult
                    .exceptionOrNull()
                    ?.printStackTrace()
            }
        }

        // ======================================================
        // CHECK CHAPTER COMPLETION
        // ======================================================

        var chapterCompleted =
            false

        try {

            val savedJourney =
                JourneyStorage.getJourney(
                    this,
                    currentJourneyId
                )

            if (savedJourney != null) {

                val response =
                    JsonParser.parseJourney(
                        savedJourney.journeyJson
                    )

                val journey =
                    response.toJourney()

                val chapter =
                    journey.chapters.find {
                        it.id == currentChapterId
                    }

                if (chapter != null) {

                    // --------------------------------------------------
                    // GET ALL LESSON IDS
                    // --------------------------------------------------

                    val lessonIds =
                        chapter.lessons.map {
                            it.id
                        }

                    // --------------------------------------------------
                    // CHECK LESSONS + CHALLENGE
                    // --------------------------------------------------

                    chapterCompleted =
                        ProgressManager
                            .checkAndCompleteChapter(
                                context = this,
                                journeyId =
                                    currentJourneyId,
                                chapterId =
                                    currentChapterId,
                                lessonIds =
                                    lessonIds,
                                challengeId =
                                    currentChallengeId
                            )

                    // ==================================================
                    // CHAPTER COMPLETED
                    // ==================================================

                    if (chapterCompleted) {

                        lifecycleScope.launch {

                            // --------------------------------------------------
                            // FIREBASE CHAPTER COMPLETION
                            // --------------------------------------------------

                            val chapterResult =
                                FirebaseProgressManager
                                    .saveChapterCompletion(
                                        journeyId =
                                            currentJourneyId,
                                        chapterId =
                                            currentChapterId
                                    )

                            if (
                                chapterResult.isFailure
                            ) {

                                chapterResult
                                    .exceptionOrNull()
                                    ?.printStackTrace()
                            }

                            // --------------------------------------------------
                            // FIRST CHAPTER ACHIEVEMENT
                            // --------------------------------------------------

                            val achievementResult =
                                AchievementManager
                                    .unlockAchievement(
                                        achievementId =
                                            "first_chapter",

                                        title =
                                            "First Chapter",

                                        description =
                                            "Complete your first chapter."
                                    )

                            if (
                                achievementResult.isFailure
                            ) {

                                achievementResult
                                    .exceptionOrNull()
                                    ?.printStackTrace()
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {

            e.printStackTrace()
        }

        // ======================================================
        // RESULT MESSAGE
        // ======================================================

        feedbackText.visibility =
            View.VISIBLE

        feedbackText.text =
            if (chapterCompleted) {

                "✓ Challenge completed!\n\n" +
                        "Score: $percentage%\n\n" +
                        "+$earnedXp XP earned.\n\n" +
                        "🏆 Chapter completed!"

            } else {

                "✓ Challenge completed!\n\n" +
                        "Score: $percentage%\n\n" +
                        "+$earnedXp XP earned."
            }

        // ======================================================
        // FINAL BUTTON
        // ======================================================

        if (chapterCompleted) {

            // --------------------------------------------------
            // CHAPTER COMPLETED
            // --------------------------------------------------

            completeButton.text =
                "CHAPTER COMPLETED   →"

            completeButton.isEnabled =
                true

            completeButton.setOnClickListener {

                val intent =
                    Intent(
                        this,
                        ChapterActivity::class.java
                    )

                intent.putExtra(
                    "JOURNEY_ID",
                    currentJourneyId
                )

                intent.putExtra(
                    "CHAPTER_ID",
                    currentChapterId
                )

                startActivity(intent)

                finish()
            }

        } else {

            // --------------------------------------------------
            // CHALLENGE COMPLETED
            // --------------------------------------------------

            completeButton.text =
                "COMPLETED"

            completeButton.isEnabled =
                false
        }
    }

    // ======================================================
    // DP
    // ======================================================

    private fun Int.dp(): Int {

        return (
                this *
                        resources.displayMetrics.density
                ).toInt()
    }
}
