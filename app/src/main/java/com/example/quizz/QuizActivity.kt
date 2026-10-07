package com.example.quizz

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.quizz.data.model.QuizQuestion
import com.example.quizz.data.remote.JsonParser
import com.example.quizz.data.remote.toJourney
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class QuizActivity : AppCompatActivity() {

    private lateinit var questionCounter: TextView
    private lateinit var questionText: TextView
    private lateinit var optionsGroup: RadioGroup
    private lateinit var feedbackText: TextView
    private lateinit var checkButton: MaterialButton

    private lateinit var progressBar: ProgressBar

    private var questions: List<QuizQuestion> =
        emptyList()
    private lateinit var playerAvatar: ImageView


    private var currentQuestionIndex = 0
    private var score = 0
    private var answerChecked = false

    private var journeyId: String? = null
    private var lessonId: String? = null
    private var chapterId: String? = null

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)


        setContentView(
            R.layout.activity_quiz
        )
        CosmeticThemeManager.apply(this)

        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0


        questionCounter =
            findViewById(R.id.questionCounter)

        progressBar =
            findViewById(R.id.ProgressBar)

        questionText =
            findViewById(R.id.questionText)

        optionsGroup =
            findViewById(R.id.optionsGroup)

        feedbackText =
            findViewById(R.id.feedbackText)

        checkButton =
            findViewById(R.id.checkButton)

        playerAvatar = findViewById(R.id.playerAvatar)


        CosmeticThemeManager.stylePrimaryAction(checkButton)

        journeyId =
            intent.getStringExtra("JOURNEY_ID")

        lessonId =
            intent.getStringExtra("LESSON_ID")

        chapterId =
            intent.getStringExtra("CHAPTER_ID")

        if (
            journeyId.isNullOrEmpty() ||
            chapterId.isNullOrEmpty() ||
            lessonId.isNullOrEmpty()
        ) {
            finish()
            return
        }

        checkButton.setOnClickListener {

            if (answerChecked) {
                goToNextQuestion()
            } else {
                checkAnswer()
            }
        }




        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    return@addOnSuccessListener
                }



                val level =
                    document.getLong("TotaleLevel")
                        ?.toInt()
                        ?: 1


                val cosmeticId = document.getString("selectedCosmetic")
                    ?: FirebaseProgressManager.DEFAULT_COSMETIC

                CosmeticThemeManager.cacheSelectedCosmetic(this, cosmeticId)
                CosmeticThemeManager.apply(this)
                updatePlayerAvatar(level, cosmeticId)




            }


        loadQuiz()




    }

    // =============================================================
    // UPDATE PLAYER AVATAR
    // =============================================================

    private fun updatePlayerAvatar(
        level: Int,
        cosmeticId: String
    ) {

        val imageRes =
            CosmeticThemeManager.avatarForLevel(
                level,
                cosmeticId
            )

        playerAvatar.setImageResource(imageRes)
        playerAvatar.setPadding(0, 0, 0, 0)

    }




    private fun loadQuiz() {

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

            val currentLessonId =
                lessonId
                    ?: throw Exception(
                        "Lesson ID is missing"
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

            /*
             * The new structure is:
             *
             * Chapter
             *    └── Lesson
             *          └── Quiz
             *
             * Therefore we find the lesson first.
             */
            val lesson =
                chapter.lessons.find {
                    it.id == currentLessonId
                }

            if (lesson == null) {
                throw Exception(
                    "Lesson not found"
                )
            }

            val quiz =
                lesson.quiz

            if (quiz.questions.isEmpty()) {
                throw Exception(
                    "No quiz questions available"
                )
            }

            questions =
                quiz.questions

            showQuestion()

        } catch (e: Exception) {

            e.printStackTrace()

            finish()
        }
    }



    private fun showQuestion() {

        if (questions.isEmpty()) {
            finish()
            return
        }

        val question =
            questions[currentQuestionIndex]

        val currentQuestion =
            currentQuestionIndex + 1

        val totalQuestions =
            questions.size

        // =========================================================
        // QUESTION COUNTER
        // =========================================================

        questionCounter.text =
            "QUESTION $currentQuestion / $totalQuestions"

        // =========================================================
        // PROGRESS
        // =========================================================

        val progress =
            (currentQuestion * 100) / totalQuestions

        progressBar.progress =
            progress

        // =========================================================
        // QUESTION
        // =========================================================

        questionText.text =
            question.question

        // =========================================================
        // RESET OPTIONS
        // =========================================================

        optionsGroup.clearCheck()
        optionsGroup.removeAllViews()

        // =========================================================
        // CREATE LONG ANSWER BUTTONS
        // =========================================================

        question.options.forEachIndexed { index, option ->

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

            radioButton.buttonTintList =
                android.content.res.ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    ),
                    intArrayOf(
                        Color.parseColor("#6EA8FF"),
                        Color.parseColor("#626D89")
                    )
                )

            // =====================================================
            // LONG OPTION SIZE
            // =====================================================

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

            // =====================================================
            // INTERNAL SPACING
            // =====================================================

            radioButton.setPadding(
                20.dp(),
                0,
                20.dp(),
                0
            )

            radioButton.minHeight =
                72.dp()

            radioButton.gravity =
                android.view.Gravity.CENTER_VERTICAL

            radioButton.maxLines =
                3

            radioButton.background =
                createOptionBackground()

            // =====================================================
            // SELECT OPTION
            // =====================================================

            radioButton.setOnClickListener {

                updateOptionStates()
            }

            optionsGroup.addView(
                radioButton
            )
        }

        // =========================================================
        // RESET FEEDBACK
        // =========================================================

        feedbackText.visibility =
            View.GONE

        feedbackText.text =
            ""

        // =========================================================
        // RESET BUTTON
        // =========================================================

        checkButton.text =
            "CHECK ANSWER   →"

        answerChecked =
            false

        // Make sure the first state is correct
        updateOptionStates()
    }

    private fun updateOptionStates() {

        for (i in 0 until optionsGroup.childCount) {

            val button =
                optionsGroup.getChildAt(i)
                        as? RadioButton
                    ?: continue

            val background =
                createOptionBackground()

            if (button.isChecked) {

                background.setColor(
                    Color.parseColor("#162544")
                )

                background.setStroke(
                    2.dp(),
                    Color.parseColor("#4F8CFF")
                )

                button.setTextColor(
                    Color.parseColor("#F5F7FF")
                )

            } else {

                background.setColor(
                    Color.parseColor("#101629")
                )

                background.setStroke(
                    1.dp(),
                    Color.parseColor("#283452")
                )

                button.setTextColor(
                    Color.parseColor("#DCE2F2")
                )
            }

            button.background =
                background
        }
    }




    private fun createOptionBackground(): android.graphics.drawable.GradientDrawable {

        return android.graphics.drawable.GradientDrawable().apply {

            shape =
                android.graphics.drawable.GradientDrawable.RECTANGLE

            cornerRadius =
                18.dp().toFloat()

            setColor(
                Color.parseColor("#101629")
            )

            setStroke(
                1.dp(),
                Color.parseColor("#283452")
            )
        }
    }





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
            findViewById<RadioButton>(selectedId)

        val selectedIndex =
            selectedButton.tag as? Int
                ?: return

        val question =
            questions[currentQuestionIndex]

        val isCorrect =
            selectedIndex == question.correctAnswer

        if (isCorrect) {

            score++

            feedbackText.text =
                "✓ Correct!\n\n${question.explanation}"

        } else {

            feedbackText.text =
                "✗ Incorrect.\n\n${question.explanation}"
        }

        feedbackText.visibility =
            View.VISIBLE

        // Prevent changing the answer after checking
        for (i in 0 until optionsGroup.childCount) {

            optionsGroup
                .getChildAt(i)
                .isEnabled = false
        }

        answerChecked =
            true

        checkButton.text =
            if (
                currentQuestionIndex ==
                questions.lastIndex
            ) {
                "FINISH QUIZ   →"
            } else {
                "NEXT QUESTION   →"
            }
    }




    private fun goToNextQuestion() {

        if (
            currentQuestionIndex <
            questions.lastIndex
        ) {

            currentQuestionIndex++

            showQuestion()

        } else {

            showResult()
        }
    }

    private fun showResult() {

        val intent =
            Intent(
                this,
                ResultActivity::class.java
            )

        intent.putExtra(
            "JOURNEY_ID",
            journeyId
        )

        intent.putExtra(
            "SCORE",
            score
        )

        intent.putExtra(
            "TOTAL",
            questions.size
        )

        intent.putExtra(
            "LESSON_ID",
            lessonId
        )

        intent.putExtra(
            "CHAPTER_ID",
            chapterId
        )

        startActivity(intent)

        finish()
    }

    private fun Int.dp(): Int {

        return (
                this *
                        resources.displayMetrics.density
                ).toInt()
    }
}
