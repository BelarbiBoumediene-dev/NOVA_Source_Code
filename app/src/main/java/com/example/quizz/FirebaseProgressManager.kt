package com.example.quizz

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FirebaseProgressManager {

    private val auth =
        FirebaseAuth.getInstance()

    private val db =
        FirebaseFirestore.getInstance()

    // =====================================================
    // USER DOCUMENT
    // =====================================================

    private fun userDocument() =
        auth.currentUser?.uid?.let { uid ->

            db.collection("users")
                .document(uid)
        }

    // =====================================================
    // ADD XP TO THE USER TOTALS
    // =====================================================

    suspend fun addToTotalProgress(
        earnedXp: Int,
        earnedCoins: Int
    ): Result<Pair<Int, Int>> {

        return try {

            val userDoc =
                userDocument()
                    ?: return Result.failure(
                        Exception(
                            "User is not signed in"
                        )
                    )

            if (earnedXp <= 0) {
                val snapshot = userDoc.get().await()
                val totalXp = snapshot.getLong("TotaleXp")?.toInt() ?: 0

                return Result.success(
                    Pair(totalXp, (totalXp / XP_PER_LEVEL) + 1)
                )
            }

            val totals = db.runTransaction { transaction ->
                val snapshot = transaction.get(userDoc)
                val currentXp = snapshot.getLong("TotaleXp")?.toInt() ?: 0
                val currentCoins = snapshot.getLong("Coins")?.toInt() ?: 0
                val weekId = currentWeekId()
                val currentWeeklyXp =
                    if (snapshot.getString("weeklyXpWeek") == weekId) {
                        snapshot.getLong("weeklyXp")?.toInt() ?: 0
                    } else {
                        0
                    }
                val totalXp = currentXp + earnedXp
                val totalLevel = (totalXp / XP_PER_LEVEL) + 1

                transaction.set(
                    userDoc,
                    mapOf(
                        "TotaleXp" to totalXp,
                        "TotaleLevel" to totalLevel,
                        "Coins" to (currentCoins + earnedCoins),
                        "weeklyXp" to (currentWeeklyXp + earnedXp),
                        "weeklyXpWeek" to weekId,
                        "displayName" to (
                            snapshot.getString("displayName")
                                ?: auth.currentUser?.displayName
                                ?: "Player"
                            )
                    ),
                    SetOptions.merge()
                )

                Pair(totalXp, totalLevel)
            }.await()



            if (totals.first >= 1_000) {
                val achievementResult =
                    AchievementManager.unlockAchievement(
                        achievementId = "xp_legend",
                        title = "XP Legend",
                        description = "Earn 1,000 total XP."
                    )

                if (achievementResult.isFailure) {
                    achievementResult
                        .exceptionOrNull()
                        ?.printStackTrace()
                }
            }

            Result.success(totals)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    private const val XP_PER_LEVEL = 100

    fun currentWeekId(): String {
        return SimpleDateFormat("YYYY-'W'ww", Locale.US).format(Date())
    }

    suspend fun getCosmeticProfile(): Result<CosmeticProfile> {
        return try {
            val userDoc = userDocument()
                ?: return Result.failure(Exception("User is not signed in"))
            val snapshot = userDoc.get().await()
            val owned = snapshot.get("ownedCosmetics") as? List<*>

            Result.success(
                CosmeticProfile(
                    coins = snapshot.getLong("Coins")?.toInt() ?: 0,
                    ownedCosmetics = owned?.filterIsInstance<String>()?.toSet()
                        ?: setOf(DEFAULT_COSMETIC),
                    selectedCosmetic = snapshot.getString("selectedCosmetic")
                        ?: DEFAULT_COSMETIC
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun purchaseCosmetic(
        cosmeticId: String,
        cost: Int
    ): Result<CosmeticProfile> {
        return try {
            val userDoc = userDocument()
                ?: return Result.failure(Exception("User is not signed in"))

            val profile = db.runTransaction { transaction ->
                val snapshot = transaction.get(userDoc)
                val coins = snapshot.getLong("Coins")?.toInt() ?: 0
                val owned = (snapshot.get("ownedCosmetics") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.toMutableSet()
                    ?: mutableSetOf(DEFAULT_COSMETIC)

                val alreadyOwned = cosmeticId in owned

                if (!alreadyOwned && coins < cost) {
                    throw IllegalStateException("Not enough Coins")
                }

                owned.add(cosmeticId)
                val remainingCoins = if (alreadyOwned) coins else coins - cost

                transaction.set(
                    userDoc,
                    mapOf(
                        "Coins" to remainingCoins,
                        "ownedCosmetics" to owned.toList(),
                        "selectedCosmetic" to cosmeticId
                    ),
                    SetOptions.merge()
                )

                CosmeticProfile(remainingCoins, owned, cosmeticId)
            }.await()

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class CosmeticProfile(
        val coins: Int,
        val ownedCosmetics: Set<String>,
        val selectedCosmetic: String
    )

    const val DEFAULT_COSMETIC = "nova_default"

    data class SeekerQuestProfile(
        val isVerified: Boolean,
        val walletAddress: String?,
        val completedToday: Boolean
    )

    data class DailyQuestResult(
        val wasCompletedNow: Boolean,
        val earnedCoins: Int
    )

    suspend fun getSeekerQuestProfile(): Result<SeekerQuestProfile> {
        return try {
            val userDoc = userDocument()
                ?: return Result.failure(Exception("User is not signed in"))
            val date = currentDayId()
            val snapshot = userDoc.get().await()
            val dailySnapshot = userDoc.collection("seekerQuests")
                .document(date)
                .get()
                .await()

            Result.success(
                SeekerQuestProfile(
                    isVerified = snapshot.getBoolean("seekerVerified") ?: false,
                    walletAddress = snapshot.getString("seekerWalletAddress"),
                    completedToday = dailySnapshot.getBoolean("completed") ?: false
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveSeekerVerification(
        walletAddress: String,
        transactionSignature: String
    ): Result<Unit> {
        return try {
            val userDoc = userDocument()
                ?: return Result.failure(Exception("User is not signed in"))

            userDoc.set(
                mapOf(
                    "seekerVerified" to true,
                    "seekerWalletAddress" to walletAddress,
                    "seekerVerificationSignature" to transactionSignature,
                    "seekerVerifiedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeDailySeekerQuest(): Result<DailyQuestResult> {
        return try {
            val userDoc = userDocument()
                ?: return Result.failure(Exception("User is not signed in"))
            val date = currentDayId()
            val questDoc = userDoc.collection("seekerQuests").document(date)

            val result = db.runTransaction { transaction ->
                val userSnapshot = transaction.get(userDoc)
                if (userSnapshot.getBoolean("seekerVerified") != true) {
                    return@runTransaction DailyQuestResult(false, 0)
                }

                val questSnapshot = transaction.get(questDoc)
                if (questSnapshot.getBoolean("completed") == true) {
                    return@runTransaction DailyQuestResult(false, 0)
                }

                val currentCoins = userSnapshot.getLong("Coins")?.toInt() ?: 0
                transaction.set(
                    questDoc,
                    mapOf(
                        "completed" to true,
                        "completedAt" to System.currentTimeMillis(),
                        "rewardCoins" to DAILY_SEEKER_QUEST_COINS
                    )
                )
                transaction.set(
                    userDoc,
                    mapOf("Coins" to currentCoins + DAILY_SEEKER_QUEST_COINS),
                    SetOptions.merge()
                )

                DailyQuestResult(true, DAILY_SEEKER_QUEST_COINS)
            }.await()

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun currentDayId(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private const val DAILY_SEEKER_QUEST_COINS = 5

    // =====================================================
    // DELETE JOURNEY PROGRESS
    // =====================================================

    /** Removes every known progress document for a deleted local journey. */
    suspend fun deleteJourneyProgress(journeyId: String): Result<Unit> {
        return try {
            val userDoc = userDocument()
                ?: return Result.failure(Exception("User is not signed in"))
            val journeyDoc = userDoc.collection("journeys").document(journeyId)

            val documents = buildList {
                listOf("lessons", "chapters", "challenges").forEach { collectionName ->
                    addAll(journeyDoc.collection(collectionName).get().await().documents)
                }
                add(journeyDoc.get().await())
            }

            documents.chunked(450).forEach { chunk ->
                db.batch().apply {
                    chunk.forEach { document -> delete(document.reference) }
                }.commit().await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =====================================================
    // GET USER XP + LEVEL
    // =====================================================

    suspend fun getUserProgress():
            Result<Pair<Int, Int>> {

        return try {

            val userDoc =
                userDocument()
                    ?: return Result.failure(
                        Exception(
                            "User is not signed in"
                        )
                    )

            val snapshot =
                userDoc.get().await()

            val xp =
                snapshot
                    .getLong("TotaleXp")
                    ?.toInt()
                    ?: 0

            val level =
                snapshot
                    .getLong("TotaleLevel")
                    ?.toInt()
                    ?: 1

            Result.success(
                Pair(
                    xp,
                    level
                )
            )

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // =====================================================
    // SAVE LESSON COMPLETION
    // =====================================================

    suspend fun saveLessonCompletion(
        journeyId: String,
        lessonId: String
    ): Result<Unit> {

        return try {

            val uid =
                auth.currentUser?.uid
                    ?: return Result.failure(
                        Exception(
                            "User is not signed in"
                        )
                    )

            val journeyDoc =
                db.collection("users")
                    .document(uid)
                    .collection("journeys")
                    .document(journeyId)

            val data =
                mapOf(
                    "completed" to true
                )

            journeyDoc
                .collection("lessons")
                .document(lessonId)
                .set(data)
                .await()

            Result.success(Unit)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // =====================================================
    // SAVE CHAPTER COMPLETION
    // =====================================================

    suspend fun saveChapterCompletion(
        journeyId: String,
        chapterId: String
    ): Result<Unit> {

        return try {

            val uid =
                auth.currentUser?.uid
                    ?: return Result.failure(
                        Exception(
                            "User is not signed in"
                        )
                    )

            val chapterDoc =
                db.collection("users")
                    .document(uid)
                    .collection("journeys")
                    .document(journeyId)
                    .collection("chapters")
                    .document(chapterId)

            val data =
                mapOf(
                    "completed" to true
                )

            chapterDoc
                .set(data)
                .await()

            Result.success(Unit)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // =====================================================
    // SAVE CHALLENGE COMPLETION
    // =====================================================

    suspend fun saveChallengeCompletion(
        journeyId: String,
        challengeId: String
    ): Result<Int> {

        return try {

            val uid =
                auth.currentUser?.uid
                    ?: return Result.failure(
                        Exception("User is not signed in")
                    )

            val journeyDoc =
                db.collection("users")
                    .document(uid)
                    .collection("journeys")
                    .document(journeyId)

            // ------------------------------------------
            // CHECK IF THIS CHALLENGE WAS ALREADY SAVED
            // ------------------------------------------

            val challengeDoc =
                journeyDoc
                    .collection("challenges")
                    .document(challengeId)

            val existingChallenge =
                challengeDoc.get().await()

            if (existingChallenge.exists()) {

                val userSnapshot =
                    db.collection("users")
                        .document(uid)
                        .get()
                        .await()

                val currentCount =
                    userSnapshot
                        .getLong("challengesCompleted")
                        ?.toInt()
                        ?: 0

                return Result.success(
                    currentCount
                )
            }

            // ------------------------------------------
            // SAVE CHALLENGE COMPLETION
            // ------------------------------------------

            challengeDoc
                .set(
                    mapOf(
                        "completed" to true
                    )
                )
                .await()

            // ------------------------------------------
            // GET CURRENT COUNTER
            // ------------------------------------------

            val userDoc =
                db.collection("users")
                    .document(uid)

            val userSnapshot =
                userDoc.get().await()

            val currentCount =
                userSnapshot
                    .getLong("challengesCompleted")
                    ?.toInt()
                    ?: 0

            val newCount =
                currentCount + 1

            // ------------------------------------------
            // SAVE NEW COUNTER
            // ------------------------------------------

            userDoc.set(
                mapOf(
                    "challengesCompleted" to newCount
                ),
                SetOptions.merge()
            ).await()

            // ------------------------------------------
            // UNLOCK CHALLENGE HUNTER
            // ------------------------------------------

            if (newCount >= 5) {

                val achievementResult =
                    AchievementManager.unlockAchievement(
                        achievementId =
                            "challenge_hunter",

                        title =
                            "Challenge Hunter",

                        description =
                            "Complete 5 challenges."
                    )

                if (achievementResult.isFailure) {

                    achievementResult
                        .exceptionOrNull()
                        ?.printStackTrace()
                }
            }

            Result.success(
                newCount
            )

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // =====================================================
    // INCREMENT PASSED QUIZZES
    // =====================================================

    suspend fun incrementPassedQuiz():
            Result<Int> {

        return try {

            val uid =
                auth.currentUser?.uid
                    ?: return Result.failure(
                        Exception(
                            "User is not signed in"
                        )
                    )

            val userDoc =
                db.collection("users")
                    .document(uid)

            // ---------------------------------------------
            // GET CURRENT COUNT
            // ---------------------------------------------

            val snapshot =
                userDoc.get().await()

            val currentCount =
                snapshot
                    .getLong("quizzesPassed")
                    ?.toInt()
                    ?: 0

            // ---------------------------------------------
            // INCREMENT
            // ---------------------------------------------

            val newCount =
                currentCount + 1

            // ---------------------------------------------
            // SAVE COUNT
            // ---------------------------------------------

            userDoc.set(
                mapOf(
                    "quizzesPassed" to newCount
                ),
                SetOptions.merge()
            ).await()

            // ---------------------------------------------
            // QUIZ MASTER
            // ---------------------------------------------

            if (newCount >= 10) {

                val achievementResult =
                    AchievementManager
                        .unlockAchievement(
                            achievementId =
                                "quiz_master",

                            title =
                                "Quiz Master",

                            description =
                                "Pass 10 quizzes."
                        )

                if (achievementResult.isFailure) {

                    achievementResult
                        .exceptionOrNull()
                        ?.printStackTrace()
                }
            }

            if (newCount >= 50) {

                val achievementResult =
                    AchievementManager
                        .unlockAchievement(
                            achievementId =
                                "quiz_grandmaster",

                            title =
                                "Quiz Grandmaster",

                            description =
                                "Pass 50 quizzes."
                        )

                if (achievementResult.isFailure) {

                    achievementResult
                        .exceptionOrNull()
                        ?.printStackTrace()
                }
            }

            Result.success(
                newCount
            )

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}
