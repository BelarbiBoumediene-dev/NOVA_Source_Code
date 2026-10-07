package com.example.quizz

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.quizz.data.remote.JsonParser
import com.example.quizz.data.remote.toJourney
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LessonActivity : AppCompatActivity() {

    private lateinit var lessonTitle: TextView
    private lateinit var lessonContent: TextView
    private lateinit var lessonExample: TextView
    private lateinit var lessonPractice: TextView
    private lateinit var lessonTime: TextView
    private lateinit var practiceButton: MaterialButton

    private lateinit var playerAvatar: ImageView


    private var journeyId: String? = null
    private var chapterId: String? = null
    private var lessonId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_lesson)
        CosmeticThemeManager.apply(this)

        // =====================================================
        // SYSTEM UI
        // =====================================================

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0

        initializeViews()
        readIntentData()

        if (!hasValidIds()) {
            finish()
            return
        }


        playerAvatar = findViewById(R.id.playerAvatar)




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

        loadLesson()
        setupListeners()
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


    // =========================================================
    // RESUME
    // =========================================================

    override fun onResume() {
        super.onResume()

        if (::lessonTitle.isInitialized && hasValidIds()) {
            loadLesson()
        }
    }

    // =========================================================
    // VIEWS
    // =========================================================

    private fun initializeViews() {

        lessonTitle =
            findViewById(R.id.lessonTitle)

        lessonContent =
            findViewById(R.id.lessonContent)

        lessonExample =
            findViewById(R.id.lessonExample)

        lessonPractice =
            findViewById(R.id.lessonPractice)

        lessonTime =
            findViewById(R.id.lessonTime)

        practiceButton =
            findViewById(R.id.practiceButton)

        CosmeticThemeManager.stylePrimaryAction(practiceButton)
    }

    // =========================================================
    // INTENT DATA
    // =========================================================

    private fun readIntentData() {

        journeyId =
            intent.getStringExtra("JOURNEY_ID")

        chapterId =
            intent.getStringExtra("CHAPTER_ID")

        lessonId =
            intent.getStringExtra("LESSON_ID")
    }

    private fun hasValidIds(): Boolean {

        return !journeyId.isNullOrEmpty() &&
                !chapterId.isNullOrEmpty() &&
                !lessonId.isNullOrEmpty()
    }

    // =========================================================
    // BUTTON
    // =========================================================

    private fun setupListeners() {

        practiceButton.setOnClickListener {

            val currentJourneyId =
                journeyId ?: return@setOnClickListener

            val currentChapterId =
                chapterId ?: return@setOnClickListener

            val currentLessonId =
                lessonId ?: return@setOnClickListener

            val quizIntent =
                Intent(
                    this,
                    QuizActivity::class.java
                )

            quizIntent.putExtra(
                "JOURNEY_ID",
                currentJourneyId
            )

            quizIntent.putExtra(
                "CHAPTER_ID",
                currentChapterId
            )

            quizIntent.putExtra(
                "LESSON_ID",
                currentLessonId
            )

            startActivity(quizIntent)
        }
    }

    // =========================================================
    // LOAD LESSON
    // =========================================================

    private fun loadLesson() {

        try {

            val currentJourneyId =
                journeyId ?: return

            val currentChapterId =
                chapterId ?: return

            val currentLessonId =
                lessonId ?: return

            // -------------------------------------------------
            // GET SAVED JOURNEY
            // -------------------------------------------------

            val savedJourney =
                JourneyStorage.getJourney(
                    this,
                    currentJourneyId
                )

            if (savedJourney == null) {
                finish()
                return
            }

            // -------------------------------------------------
            // PARSE JOURNEY
            // -------------------------------------------------

            val response =
                JsonParser.parseJourney(
                    savedJourney.journeyJson
                )

            val journey =
                response.toJourney()

            // -------------------------------------------------
            // FIND CHAPTER
            // -------------------------------------------------

            val chapter =
                journey.chapters.find { chapter ->

                    chapter.id == currentChapterId
                }

            if (chapter == null) {
                finish()
                return
            }

            // -------------------------------------------------
            // FIND LESSON
            // -------------------------------------------------

            val lesson =
                chapter.lessons.find { lesson ->

                    lesson.id == currentLessonId
                }

            if (lesson == null) {
                finish()
                return
            }

            // =================================================
            // DISPLAY LESSON
            // =================================================

            lessonTitle.text =
                lesson.title

            lessonContent.text =
                lesson.content

            lessonExample.text =
                lesson.example

            lessonPractice.text =
                lesson.practice

            lessonTime.text =
                "${lesson.estimatedMinutes} min"

        } catch (e: Exception) {

            e.printStackTrace()

            finish()
        }
    }
}
