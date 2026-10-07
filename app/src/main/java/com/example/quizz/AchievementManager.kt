package com.example.quizz

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AchievementManager {

    private val auth =
        FirebaseAuth.getInstance()

    private val db =
        FirebaseFirestore.getInstance()

    // =====================================================
    // USER ACHIEVEMENTS COLLECTION
    // =====================================================

    private fun userAchievementsCollection() =
        auth.currentUser?.uid?.let { uid ->

            db.collection("users")
                .document(uid)
                .collection("achievements")
        }

    // =====================================================
    // UNLOCK ACHIEVEMENT
    // =====================================================

    suspend fun unlockAchievement(
        achievementId: String,
        title: String,
        description: String
    ): Result<Boolean> {

        return try {

            val collection =
                userAchievementsCollection()
                    ?: return Result.failure(
                        Exception(
                            "User is not signed in"
                        )
                    )

            val achievementDoc =
                collection.document(
                    achievementId
                )

            // ---------------------------------------------
            // CHECK IF ALREADY UNLOCKED
            // ---------------------------------------------

            val snapshot =
                achievementDoc.get().await()

            if (snapshot.exists()) {

                return Result.success(false)
            }

            // ---------------------------------------------
            // ACHIEVEMENT DATA
            // ---------------------------------------------

            val data =
                mapOf(
                    "title" to title,
                    "description" to description,
                    "unlocked" to true,
                    "unlockedAt" to System.currentTimeMillis()
                )

            // ---------------------------------------------
            // SAVE
            // ---------------------------------------------

            achievementDoc
                .set(data)
                .await()

            AchievementNotifier.show(
                title = title,
                description = description
            )

            Result.success(true)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}
