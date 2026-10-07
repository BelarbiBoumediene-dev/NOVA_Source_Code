package com.example.quizz

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.quizz.data.remote.JsonParser
import com.example.quizz.data.remote.toJourney
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import android.widget.ImageView

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_result)
        CosmeticThemeManager.apply(this)

        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0

        // =====================================================
        // VIEWS
        // =====================================================

        val scoreText =
            findViewById<TextView>(R.id.scoreText)

        val resultMessage =
            findViewById<TextView>(R.id.resultMessage)

        val xpText =
            findViewById<TextView>(R.id.xpText)

        val coinsText =
            findViewById<TextView>(R.id.coinsText)

        val doneButton =
            findViewById<MaterialButton>(R.id.doneButton)

        // =====================================================
        // INTENT DATA
        // =====================================================

        val journeyId =
            intent.getStringExtra("JOURNEY_ID")

        val score =
            intent.getIntExtra("SCORE", 0)

        val total =
            intent.getIntExtra("TOTAL", 1)

        val lessonId =
            intent.getStringExtra("LESSON_ID")

        val chapterId =
            intent.getStringExtra("CHAPTER_ID")

        // =====================================================
        // VALIDATION
        // =====================================================

        if (
            journeyId.isNullOrEmpty() ||
            lessonId.isNullOrEmpty() ||
            chapterId.isNullOrEmpty()
        ) {
            finish()
            return
        }

        val currentJourneyId = journeyId
        val currentLessonId = lessonId
        val currentChapterId = chapterId

        // =====================================================
        // CALCULATE SCORE
        // =====================================================

        val percentage =
            if (total > 0) {
                (score * 100) / total
            } else {
                0
            }

        val resultImage =
            findViewById<ImageView>(R.id.resultImage)

        resultImage.setImageResource(
            CosmeticThemeManager.resultImageForScore(
                percentage,
                CosmeticThemeManager.selectedCosmetic(this)
            )
        )


        animateScore(scoreText, percentage)

        // =====================================================
        // OLD XP
        // =====================================================

        val oldXp =
            ProgressManager.getXp(
                this,
                currentJourneyId
            )

        val oldLevel =
            (oldXp / 100) + 1

        // =====================================================
        // DEFAULT XP
        // =====================================================

        var earnedXp = 0
        var earnedCoins = 0

        // =====================================================
        // QUIZ PASSED
        // =====================================================

        val quizPassed =
            percentage >= 70

        if (quizPassed) {

            ProgressManager.completeQuiz(
                this,
                currentJourneyId,
                currentLessonId
            )
        }

        // =====================================================
        // RESULT MESSAGE
        // =====================================================

        resultMessage.text =
            when {

                percentage >= 80 ->
                    "Excellent work! Your knowledge is growing."

                percentage >= 70 ->
                    "Great job! You passed the quest."

                else ->
                    "Keep practicing. You can try again."
            }

        // =====================================================
        // COMPLETE LESSON + XP
        // =====================================================

        val lessonAlreadyCompleted =
            ProgressManager.isLessonCompleted(
                this,
                currentJourneyId,
                currentLessonId
            )

        if (
            quizPassed &&
            !lessonAlreadyCompleted
        ) {

            // ---------------------------------------------
            // XP RULE
            // ---------------------------------------------
            // Every correct answer = 10 XP

            earnedXp =
                score * 10

            earnedCoins = 10

            // ---------------------------------------------
            // LOCAL XP
            // ---------------------------------------------

            ProgressManager.addXp(
                this,
                currentJourneyId,
                earnedXp
            )

            // ---------------------------------------------
            // COMPLETE LESSON
            // ---------------------------------------------

            ProgressManager.completeLesson(
                this,
                currentJourneyId,
                currentLessonId
            )

            // ---------------------------------------------
            // FIREBASE
            // ---------------------------------------------

            lifecycleScope.launch {

                // Save lesson
                val lessonResult =
                    FirebaseProgressManager
                        .saveLessonCompletion(
                            journeyId =
                                currentJourneyId,

                            lessonId =
                                currentLessonId
                        )

                if (lessonResult.isFailure) {

                    lessonResult
                        .exceptionOrNull()
                        ?.printStackTrace()
                }

                // Count passed quiz
                val quizResult =
                    FirebaseProgressManager
                        .incrementPassedQuiz()

                if (quizResult.isFailure) {

                    quizResult
                        .exceptionOrNull()
                        ?.printStackTrace()

                } else {

                    val passedQuizCount =
                        quizResult.getOrNull() ?: 0

                    println(
                        "Passed quizzes: $passedQuizCount"
                    )
                }

                // First lesson achievement
                val achievementResult =
                    AchievementManager
                        .unlockAchievement(
                            achievementId =
                                "first_lesson",

                            title =
                                "First Lesson",

                            description =
                                "Complete your first lesson."
                        )

                if (achievementResult.isFailure) {

                    achievementResult
                        .exceptionOrNull()
                        ?.printStackTrace()
                }

                val dailyQuestResult =
                    FirebaseProgressManager.completeDailySeekerQuest()

                if (dailyQuestResult.isSuccess &&
                    dailyQuestResult.getOrNull()?.wasCompletedNow == true
                ) {
                    resultMessage.text =
                        resultMessage.text.toString() +
                                "\n\nSEEKER DAILY QUEST COMPLETE!" +
                                "\n+${dailyQuestResult.getOrNull()?.earnedCoins} Coins"
                }
            }
        }

        // =====================================================
        // CHECK CHAPTER COMPLETION
        // =====================================================

        if (quizPassed) {

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

                        val lessonIds =
                            chapter.lessons.map {
                                it.id
                            }

                        val challengeId =
                            chapter.challenge?.id

                        val chapterCompleted =
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
                                        challengeId
                                )

                        // ---------------------------------
                        // SAVE CHAPTER TO FIREBASE
                        // ---------------------------------

                        if (chapterCompleted) {

                            lifecycleScope.launch {

                                val chapterResult =
                                    FirebaseProgressManager
                                        .saveChapterCompletion(
                                            journeyId =
                                                currentJourneyId,

                                            chapterId =
                                                currentChapterId
                                        )

                                if (chapterResult.isFailure) {

                                    chapterResult
                                        .exceptionOrNull()
                                        ?.printStackTrace()
                                }

                                // First chapter achievement
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
        }

        // =====================================================
        // NEW XP
        // =====================================================

        val newXp =
            ProgressManager.getXp(
                this,
                currentJourneyId
            )

        // =====================================================
        // NEW LEVEL
        // =====================================================

        val newLevel =
            (newXp / 100) + 1

        // =====================================================
        // LEVEL UP
        // =====================================================

        if (newLevel > oldLevel) {

            resultMessage.text =
                resultMessage.text.toString() +
                        "\n\n🎉 LEVEL UP!\n" +
                        "You reached Level $newLevel!"
        }

        // =====================================================
        // ADD THE QUIZ REWARD TO THE USER TOTALS IN FIREBASE
        // =====================================================

        if (earnedXp > 0) {
            lifecycleScope.launch {

            val firebaseResult =
                FirebaseProgressManager
                    .addToTotalProgress(
                        earnedXp = earnedXp,
                        earnedCoins = earnedCoins
                    )

            if (firebaseResult.isFailure) {

                firebaseResult
                    .exceptionOrNull()
                    ?.printStackTrace()
            }
            }
        }

        // =====================================================
        // XP DISPLAY
        // =====================================================

        xpText.text =
            if (earnedXp > 0) {
                "+$earnedXp XP"
            } else {
                "+0 XP"
            }

        coinsText.text =
            if (earnedCoins > 0) {
                "+$earnedCoins Coins"
            } else {
                "No Coins this time"
            }

        animateReward(xpText)
        animateReward(coinsText)

        // =====================================================
        // DONE
        // =====================================================

        doneButton.setOnClickListener {

            finish()
        }
    }

    private fun animateScore(
        scoreText: TextView,
        target: Int
    ) {
        ValueAnimator.ofInt(0, target).apply {
            duration = 700
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                scoreText.text = "${animation.animatedValue}%"
            }
            start()
        }
    }

    private fun animateReward(view: TextView) {
        view.alpha = 0f
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(350)
            .setStartDelay(250)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
}
