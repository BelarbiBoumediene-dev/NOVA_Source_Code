package com.example.quizz

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

object ProgressManager {

    private const val PREFS_NAME =
        "ai_quest_progress"

    private const val KEY_COMPLETED_LESSONS =
        "completed_lessons"

    private const val KEY_COMPLETED_CHAPTERS =
        "completed_chapters"

    private const val KEY_COMPLETED_CHALLENGES =
        "completed_challenges"

    private const val KEY_PASSED_QUIZZES =
        "passed_quizzes"

    private fun prefs(context: Context) =
        context.getSharedPreferences(
            "${PREFS_NAME}_${currentUserScope()}",
            Context.MODE_PRIVATE
        )

    private fun currentUserScope(): String {
        return FirebaseAuth.getInstance().currentUser?.uid
            ?: "signed_out"
    }

    // --------------------------------------------------
    // KEYS
    // --------------------------------------------------

    private fun xpKey(
        journeyId: String
    ): String {
        return "xp_$journeyId"
    }

    private fun lessonKey(
        journeyId: String,
        lessonId: String
    ): String {
        return "$journeyId:$lessonId"
    }

    private fun chapterKey(
        journeyId: String,
        chapterId: String
    ): String {
        return "$journeyId:$chapterId"
    }

    private fun challengeKey(
        journeyId: String,
        challengeId: String
    ): String {
        return "$journeyId:$challengeId"
    }

    // --------------------------------------------------
    // XP
    // --------------------------------------------------

    fun getXp(
        context: Context,
        journeyId: String
    ): Int {

        return prefs(context).getInt(
            xpKey(journeyId),
            0
        )
    }

    fun addXp(
        context: Context,
        journeyId: String,
        amount: Int
    ) {

        val currentXp =
            getXp(
                context,
                journeyId
            )

        prefs(context)
            .edit()
            .putInt(
                xpKey(journeyId),
                currentXp + amount
            )
            .apply()
    }

    // --------------------------------------------------
    // LESSONS
    // --------------------------------------------------

    fun isLessonCompleted(
        context: Context,
        journeyId: String,
        lessonId: String
    ): Boolean {

        val completedLessons =
            prefs(context)
                .getStringSet(
                    KEY_COMPLETED_LESSONS,
                    emptySet()
                )
                ?: emptySet()

        return lessonKey(
            journeyId,
            lessonId
        ) in completedLessons
    }

    fun completeLesson(
        context: Context,
        journeyId: String,
        lessonId: String
    ) {

        val completedLessons =
            prefs(context)
                .getStringSet(
                    KEY_COMPLETED_LESSONS,
                    emptySet()
                )
                ?.toMutableSet()
                ?: mutableSetOf()

        completedLessons.add(
            lessonKey(
                journeyId,
                lessonId
            )
        )

        prefs(context)
            .edit()
            .putStringSet(
                KEY_COMPLETED_LESSONS,
                completedLessons
            )
            .apply()
    }

    // --------------------------------------------------
    // CHAPTERS
    // --------------------------------------------------

    fun isChapterCompleted(
        context: Context,
        journeyId: String,
        chapterId: String
    ): Boolean {

        val completedChapters =
            prefs(context)
                .getStringSet(
                    KEY_COMPLETED_CHAPTERS,
                    emptySet()
                )
                ?: emptySet()

        return chapterKey(
            journeyId,
            chapterId
        ) in completedChapters
    }

    fun completeChapter(
        context: Context,
        journeyId: String,
        chapterId: String
    ) {

        val completedChapters =
            prefs(context)
                .getStringSet(
                    KEY_COMPLETED_CHAPTERS,
                    emptySet()
                )
                ?.toMutableSet()
                ?: mutableSetOf()

        completedChapters.add(
            chapterKey(
                journeyId,
                chapterId
            )
        )

        prefs(context)
            .edit()
            .putStringSet(
                KEY_COMPLETED_CHAPTERS,
                completedChapters
            )
            .apply()
    }

    // --------------------------------------------------
    // CHALLENGES
    // --------------------------------------------------

    fun isChallengeCompleted(
        context: Context,
        journeyId: String,
        challengeId: String
    ): Boolean {

        val completedChallenges =
            prefs(context)
                .getStringSet(
                    KEY_COMPLETED_CHALLENGES,
                    emptySet()
                )
                ?: emptySet()

        return challengeKey(
            journeyId,
            challengeId
        ) in completedChallenges
    }

    fun completeChallenge(
        context: Context,
        journeyId: String,
        challengeId: String
    ) {

        val completedChallenges =
            prefs(context)
                .getStringSet(
                    KEY_COMPLETED_CHALLENGES,
                    emptySet()
                )
                ?.toMutableSet()
                ?: mutableSetOf()

        completedChallenges.add(
            challengeKey(
                journeyId,
                challengeId
            )
        )

        prefs(context)
            .edit()
            .putStringSet(
                KEY_COMPLETED_CHALLENGES,
                completedChallenges
            )
            .apply()
    }

    // --------------------------------------------------
    // LESSON QUIZZES
    // --------------------------------------------------

    /*
     * Each lesson has its own quiz.
     *
     * Therefore the quiz is identified by:
     *
     * journeyId + lessonId
     */
    fun isQuizPassed(
        context: Context,
        journeyId: String,
        lessonId: String
    ): Boolean {

        val passedQuizzes =
            prefs(context)
                .getStringSet(
                    KEY_PASSED_QUIZZES,
                    emptySet()
                )
                ?: emptySet()

        return lessonKey(
            journeyId,
            lessonId
        ) in passedQuizzes
    }

    fun completeQuiz(
        context: Context,
        journeyId: String,
        lessonId: String
    ) {

        val passedQuizzes =
            prefs(context)
                .getStringSet(
                    KEY_PASSED_QUIZZES,
                    emptySet()
                )
                ?.toMutableSet()
                ?: mutableSetOf()

        passedQuizzes.add(
            lessonKey(
                journeyId,
                lessonId
            )
        )

        prefs(context)
            .edit()
            .putStringSet(
                KEY_PASSED_QUIZZES,
                passedQuizzes
            )
            .apply()
    }

    // --------------------------------------------------
    // CHAPTER COMPLETION
    // --------------------------------------------------

    fun checkAndCompleteChapter(
        context: Context,
        journeyId: String,
        chapterId: String,
        lessonIds: List<String>,
        challengeId: String?
    ): Boolean {

        /*
         * A chapter is complete only when
         * every lesson is complete.
         *
         * A lesson becomes complete after
         * passing its own quiz.
         */
        val allLessonsCompleted =
            lessonIds.all { lessonId ->

                isLessonCompleted(
                    context,
                    journeyId,
                    lessonId
                )
            }

        /*
         * The challenge is optional.
         *
         * If the chapter has no challenge,
         * it is considered satisfied.
         */
        val challengeCompleted =
            challengeId == null ||
                    isChallengeCompleted(
                        context,
                        journeyId,
                        challengeId
                    )

        val chapterCompleted =
            allLessonsCompleted &&
                    challengeCompleted

        if (chapterCompleted) {

            completeChapter(
                context,
                journeyId,
                chapterId
            )
        }

        return chapterCompleted
    }

    // --------------------------------------------------
    // DELETE JOURNEY PROGRESS
    // --------------------------------------------------

    fun clearJourney(
        context: Context,
        journeyId: String
    ) {

        val editor =
            prefs(context).edit()

        // XP
        editor.remove(
            xpKey(journeyId)
        )

        // Lessons
        val lessons =
            prefs(context)
                .getStringSet(
                    KEY_COMPLETED_LESSONS,
                    emptySet()
                )
                ?.toMutableSet()
                ?: mutableSetOf()

        lessons.removeAll {
            it.startsWith("$journeyId:")
        }

        editor.putStringSet(
            KEY_COMPLETED_LESSONS,
            lessons
        )

        // Chapters
        val chapters =
            prefs(context)
                .getStringSet(
                    KEY_COMPLETED_CHAPTERS,
                    emptySet()
                )
                ?.toMutableSet()
                ?: mutableSetOf()

        chapters.removeAll {
            it.startsWith("$journeyId:")
        }

        editor.putStringSet(
            KEY_COMPLETED_CHAPTERS,
            chapters
        )

        // Challenges
        val challenges =
            prefs(context)
                .getStringSet(
                    KEY_COMPLETED_CHALLENGES,
                    emptySet()
                )
                ?.toMutableSet()
                ?: mutableSetOf()

        challenges.removeAll {
            it.startsWith("$journeyId:")
        }

        editor.putStringSet(
            KEY_COMPLETED_CHALLENGES,
            challenges
        )

        // Lesson quizzes
        val quizzes =
            prefs(context)
                .getStringSet(
                    KEY_PASSED_QUIZZES,
                    emptySet()
                )
                ?.toMutableSet()
                ?: mutableSetOf()

        quizzes.removeAll {
            it.startsWith("$journeyId:")
        }

        editor.putStringSet(
            KEY_PASSED_QUIZZES,
            quizzes
        )

        editor.apply()
    }
}
