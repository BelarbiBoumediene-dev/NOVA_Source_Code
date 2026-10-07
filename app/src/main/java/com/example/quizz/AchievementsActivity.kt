package com.example.quizz

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AchievementsActivity : AppCompatActivity() {

    private lateinit var achievementsContainer: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var achievementProgressText: TextView
    private lateinit var achievementPercent: TextView
    private lateinit var achievementProgress: ProgressBar

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // --------------------------------------------------
    // ALL AVAILABLE ACHIEVEMENTS
    // --------------------------------------------------

    private val allAchievements = listOf(

        AchievementDefinition(
            id = "first_lesson",
            title = "First Lesson",
            description = "Complete your first lesson."
        ),

        AchievementDefinition(
            id = "first_chapter",
            title = "First Chapter",
            description = "Complete your first chapter."
        ),

        AchievementDefinition(
            id = "quiz_master",
            title = "Quiz Master",
            description = "Pass 10 quizzes."
        ),

        AchievementDefinition(
            id = "challenge_hunter",
            title = "Challenge Hunter",
            description = "Complete 5 challenges."
        ),

        AchievementDefinition(
            id = "quiz_veteran",
            title = "Quiz Veteran",
            description = "Pass 25 quizzes."
        ),

        AchievementDefinition(
            id = "challenge_master",
            title = "Challenge Master",
            description = "Complete 10 challenges."
        ),

        AchievementDefinition(
            id = "xp_legend",
            title = "XP Legend",
            description = "Earn 1,000 total XP."
        ),

        AchievementDefinition(
            id = "quiz_grandmaster",
            title = "Quiz Grandmaster",
            description = "Pass 50 quizzes."
        ),

        AchievementDefinition(
            id = "quiz_legend",
            title = "Quiz Legend",
            description = "Pass 100 quizzes."
        ),

        AchievementDefinition(
            id = "xp_master",
            title = "XP Master",
            description = "Earn 5,000 total XP."
        )
    )

    // --------------------------------------------------
    // DATA CLASSES
    // --------------------------------------------------

    data class AchievementDefinition(
        val id: String,
        val title: String,
        val description: String
    )

    data class Achievement(
        val id: String,
        val title: String,
        val description: String,
        val unlocked: Boolean
    )

    // --------------------------------------------------
    // ON CREATE
    // --------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_achievements)
        CosmeticThemeManager.apply(this)

        SolanaAchievementManager.initialize(this)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0

        achievementsContainer =
            findViewById(R.id.achievementsContainer)

        emptyText =
            findViewById(R.id.emptyText)

        achievementProgressText =
            findViewById(R.id.achievementProgressText)

        achievementPercent =
            findViewById(R.id.achievementPercent)

        achievementProgress =
            findViewById(R.id.achievementProgress)

        setupBottomNavigation()
    }

    // --------------------------------------------------
    // BOTTOM NAVIGATION
    // --------------------------------------------------

    private fun setupBottomNavigation() {

        findViewById<View>(R.id.navHome).setOnClickListener { view ->

            animateNavigation(view)

            val intent = Intent(
                this,
                MainActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)
        }

        findViewById<View>(R.id.navJourneys).setOnClickListener { view ->

            animateNavigation(view)

            val intent = Intent(
                this,
                MyJourneysActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)
        }

        findViewById<View>(R.id.navAchievements).setOnClickListener { view ->

            animateNavigation(view)
        }

        findViewById<View>(R.id.navNova).setOnClickListener { view ->

            animateNavigation(view)

            val intent = Intent(
                this,
                LeaderboardActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)
        }
    }

    // --------------------------------------------------
    // NAVIGATION ANIMATION
    // --------------------------------------------------

    private fun animateNavigation(view: View) {

        view.animate()
            .scaleX(0.88f)
            .scaleY(0.88f)
            .setDuration(70)
            .withEndAction {

                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(130)
                    .start()
            }
            .start()
    }

    // --------------------------------------------------
    // ON RESUME
    // --------------------------------------------------

    override fun onResume() {
        super.onResume()
        loadAchievements()
    }

    // --------------------------------------------------
    // LOAD ACHIEVEMENTS
    // --------------------------------------------------

    private fun loadAchievements() {

        val user = auth.currentUser

        if (user == null) {

            Toast.makeText(
                this,
                "Please sign in first.",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        lifecycleScope.launch {

            try {

                syncAchievements(user.uid)

                val snapshot =
                    db.collection("users")
                        .document(user.uid)
                        .collection("achievements")
                        .get()
                        .await()

                val unlockedIds =
                    snapshot.documents
                        .filter {
                            it.getBoolean("unlocked") == true
                        }
                        .map {
                            it.id
                        }
                        .toSet()

                val achievements =
                    allAchievements.map { definition ->

                        Achievement(
                            id = definition.id,
                            title = definition.title,
                            description = definition.description,
                            unlocked =
                                definition.id in unlockedIds
                        )
                    }

                achievementsContainer.removeAllViews()

                emptyText.visibility = View.GONE

                updateAchievementSummary(achievements)

                achievements.forEach { achievement ->
                    addAchievementView(achievement)
                }

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    this@AchievementsActivity,
                    "Unable to load achievements.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // --------------------------------------------------
    // SUMMARY
    // --------------------------------------------------

    private fun updateAchievementSummary(
        achievements: List<Achievement>
    ) {

        val unlockedCount =
            achievements.count { it.unlocked }

        val totalCount =
            achievements.size

        val progressPercent =
            if (totalCount == 0) {
                0
            } else {
                (unlockedCount * 100) / totalCount
            }

        achievementProgressText.text =
            "$unlockedCount / $totalCount UNLOCKED"

        achievementPercent.text =
            "$progressPercent%"

        achievementProgress.progress =
            progressPercent
    }

    // --------------------------------------------------
    // SYNC ACHIEVEMENTS
    // --------------------------------------------------

    private suspend fun syncAchievements(uid: String) {

        try {

            val userDoc =
                db.collection("users")
                    .document(uid)

            val snapshot =
                userDoc.get().await()

            val quizzesPassed =
                snapshot
                    .getLong("quizzesPassed")
                    ?.toInt()
                    ?: 0

            if (quizzesPassed >= 10) {

                AchievementManager.unlockAchievement(
                    achievementId = "quiz_master",
                    title = "Quiz Master",
                    description = "Pass 10 quizzes."
                )
            }

            if (quizzesPassed >= 25) {

                AchievementManager.unlockAchievement(
                    achievementId = "quiz_veteran",
                    title = "Quiz Veteran",
                    description = "Pass 25 quizzes."
                )
            }

            if (quizzesPassed >= 50) {

                AchievementManager.unlockAchievement(
                    achievementId = "quiz_grandmaster",
                    title = "Quiz Grandmaster",
                    description = "Pass 50 quizzes."
                )
            }

            if (quizzesPassed >= 100) {

                AchievementManager.unlockAchievement(
                    achievementId = "quiz_legend",
                    title = "Quiz Legend",
                    description = "Pass 100 quizzes."
                )
            }

            val challengesCompleted =
                snapshot
                    .getLong("challengesCompleted")
                    ?.toInt()
                    ?: 0

            if (challengesCompleted >= 5) {

                AchievementManager.unlockAchievement(
                    achievementId = "challenge_hunter",
                    title = "Challenge Hunter",
                    description = "Complete 5 challenges."
                )
            }

            if (challengesCompleted >= 10) {

                AchievementManager.unlockAchievement(
                    achievementId = "challenge_master",
                    title = "Challenge Master",
                    description = "Complete 10 challenges."
                )
            }

            val totalXp =
                snapshot
                    .getLong("TotaleXp")
                    ?.toInt()
                    ?: 0

            if (totalXp >= 1_000) {

                AchievementManager.unlockAchievement(
                    achievementId = "xp_legend",
                    title = "XP Legend",
                    description = "Earn 1,000 total XP."
                )
            }

            if (totalXp >= 5_000) {

                AchievementManager.unlockAchievement(
                    achievementId = "xp_master",
                    title = "XP Master",
                    description = "Earn 5,000 total XP."
                )
            }

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    // --------------------------------------------------
    // ACHIEVEMENT CARD
    // --------------------------------------------------

    private fun addAchievementView(
        achievement: Achievement
    ) {

        val card = LinearLayout(this)

        card.orientation = LinearLayout.VERTICAL

        card.setPadding(
            dp(20),
            dp(20),
            dp(20),
            dp(20)
        )

        // --------------------------------------------------
        // CARD BACKGROUND
        // --------------------------------------------------

        val background = GradientDrawable()

        background.cornerRadius =
            dp(20).toFloat()

        background.setColor(
            Color.parseColor(
                if (achievement.unlocked) {
                    "#151B30"
                } else {
                    "#101522"
                }
            )
        )

        background.setStroke(
            dp(1),
            Color.parseColor(
                if (achievement.unlocked) {
                    "#5368C9"
                } else {
                    "#252D42"
                }
            )
        )

        card.background = background

        card.elevation =
            dp(2).toFloat()

        val cardParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        cardParams.setMargins(
            0,
            0,
            0,
            dp(14)
        )

        card.layoutParams = cardParams

        // --------------------------------------------------
        // TOP ROW
        // --------------------------------------------------

        val topRow =
            LinearLayout(this)

        topRow.orientation =
            LinearLayout.HORIZONTAL

        topRow.gravity =
            Gravity.CENTER_VERTICAL

        // --------------------------------------------------
        // ICON
        // --------------------------------------------------

        val icon =
            TextView(this)

        icon.text =
            if (achievement.unlocked) {
                "🏆"
            } else {
                "🔒"
            }

        icon.textSize =
            if (achievement.unlocked) 24f else 21f

        icon.gravity =
            Gravity.CENTER

        val iconBackground =
            GradientDrawable()

        iconBackground.cornerRadius =
            dp(14).toFloat()

        iconBackground.setColor(
            Color.parseColor(
                if (achievement.unlocked) {
                    "#202B4A"
                } else {
                    "#191D2B"
                }
            )
        )

        icon.background =
            iconBackground

        val iconParams =
            LinearLayout.LayoutParams(
                dp(50),
                dp(50)
            )

        icon.layoutParams =
            iconParams

        topRow.addView(icon)

        // --------------------------------------------------
        // TITLE AREA
        // --------------------------------------------------

        val titleArea =
            LinearLayout(this)

        titleArea.orientation =
            LinearLayout.VERTICAL

        val titleParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

        titleParams.marginStart =
            dp(14)

        titleArea.layoutParams =
            titleParams

        val titleText =
            TextView(this)

        titleText.text =
            achievement.title

        titleText.textSize =
            17f

        titleText.setTypeface(
            null,
            Typeface.BOLD
        )

        titleText.setTextColor(
            Color.parseColor(
                if (achievement.unlocked) {
                    "#F7F8FF"
                } else {
                    "#A3A8BA"
                }
            )
        )

        titleArea.addView(titleText)

        val statusText =
            TextView(this)

        statusText.text =
            if (achievement.unlocked) {
                "UNLOCKED"
            } else {
                "LOCKED"
            }

        statusText.textSize =
            9f

        statusText.setTypeface(
            null,
            Typeface.BOLD
        )

        statusText.letterSpacing =
            0.10f

        statusText.setTextColor(
            Color.parseColor(
                if (achievement.unlocked) {
                    "#6EA8FF"
                } else {
                    "#5D6478"
                }
            )
        )

        val statusParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        statusParams.topMargin =
            dp(5)

        statusText.layoutParams =
            statusParams

        titleArea.addView(statusText)

        topRow.addView(titleArea)

        card.addView(topRow)

        // --------------------------------------------------
        // DESCRIPTION
        // --------------------------------------------------

        val descriptionText =
            TextView(this)

        descriptionText.text =
            achievement.description

        descriptionText.textSize =
            13f

        descriptionText.setTextColor(
            Color.parseColor("#7F879E")
        )

        descriptionText.setLineSpacing(
            0f,
            1.12f
        )

        val descriptionParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        descriptionParams.topMargin =
            dp(14)

        descriptionText.layoutParams =
            descriptionParams

        card.addView(descriptionText)

        // --------------------------------------------------
        // SOLANA
        // --------------------------------------------------

        if (achievement.unlocked) {

            addSolanaSection(
                card,
                achievement
            )
        }

        achievementsContainer.addView(card)
    }

    // --------------------------------------------------
    // SOLANA SECTION
    // --------------------------------------------------

    private fun addSolanaSection(
        card: LinearLayout,
        achievement: Achievement
    ) {

        val divider =
            View(this)

        divider.setBackgroundColor(
            Color.parseColor("#252D43")
        )

        val dividerParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            )

        dividerParams.topMargin =
            dp(17)

        dividerParams.bottomMargin =
            dp(14)

        divider.layoutParams =
            dividerParams

        card.addView(divider)

        val solanaLabel =
            TextView(this)

        solanaLabel.text =
            "◆  ON-CHAIN VERIFICATION"

        solanaLabel.textSize =
            9f

        solanaLabel.setTypeface(
            null,
            Typeface.BOLD
        )

        solanaLabel.letterSpacing =
            0.10f

        solanaLabel.setTextColor(
            Color.parseColor("#8B7CFF")
        )

        card.addView(solanaLabel)

        val solanaStatus =
            TextView(this)

        solanaStatus.textSize =
            12f

        solanaStatus.setTypeface(
            null,
            Typeface.BOLD
        )

        solanaStatus.setTextColor(
            Color.parseColor("#8C94A9")
        )

        val statusParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        statusParams.topMargin =
            dp(8)

        solanaStatus.layoutParams =
            statusParams

        card.addView(solanaStatus)

        // --------------------------------------------------
        // VERIFY BUTTON
        // --------------------------------------------------

        val verifyButton =
            TextView(this)

        verifyButton.text =
            "🔗  Verify with Solana Wallet"

        verifyButton.textSize =
            13f

        verifyButton.setTypeface(
            null,
            Typeface.BOLD
        )

        verifyButton.setTextColor(
            Color.WHITE
        )

        verifyButton.gravity =
            Gravity.CENTER

        verifyButton.setPadding(
            dp(14),
            dp(13),
            dp(14),
            dp(13)
        )

        val buttonBackground =
            GradientDrawable()

        buttonBackground.cornerRadius =
            dp(13).toFloat()

        buttonBackground.setColor(
            Color.parseColor("#202B4A")
        )

        buttonBackground.setStroke(
            dp(1),
            Color.parseColor("#34456F")
        )

        verifyButton.background =
            buttonBackground

        val buttonParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
            )

        buttonParams.topMargin =
            dp(12)

        verifyButton.layoutParams =
            buttonParams

        card.addView(verifyButton)

        // --------------------------------------------------
        // LOAD STATE
        // --------------------------------------------------

        lifecycleScope.launch {

            try {

                val user =
                    auth.currentUser
                        ?: return@launch

                val achievementDocument =
                    db.collection("users")
                        .document(user.uid)
                        .collection("achievements")
                        .document(achievement.id)
                        .get()
                        .await()

                val verified =
                    achievementDocument
                        .getBoolean("solanaVerified")
                        ?: false

                val walletAddress =
                    achievementDocument
                        .getString("walletAddress")

                val transactionSignature =
                    achievementDocument
                        .getString("transactionSignature")

                if (
                    verified &&
                    !walletAddress.isNullOrBlank()
                ) {

                    solanaStatus.text =
                        "✓  VERIFIED ON SOLANA DEVNET\n" +
                                shortenWalletAddress(
                                    walletAddress
                                )

                    solanaStatus.setTextColor(
                        Color.parseColor("#67D7A4")
                    )

                    if (
                        !transactionSignature.isNullOrBlank()
                    ) {

                        verifyButton.text =
                            "✓  View on Solana Explorer"

                        verifyButton.isEnabled =
                            true

                        verifyButton.alpha =
                            1f

                        verifyButton.setOnClickListener {

                            openSolanaExplorer(
                                transactionSignature
                            )
                        }

                    } else {

                        verifyButton.text =
                            "✓  Verified on Solana"

                        verifyButton.isEnabled =
                            false

                        verifyButton.alpha =
                            0.65f
                    }

                } else {

                    solanaStatus.text =
                        "Not verified on Solana Devnet."

                    solanaStatus.setTextColor(
                        Color.parseColor("#7F879E")
                    )
                }

            } catch (e: Exception) {

                e.printStackTrace()

                solanaStatus.text =
                    "Solana verification not available."
            }
        }

        // --------------------------------------------------
        // VERIFY CLICK
        // --------------------------------------------------

        verifyButton.setOnClickListener {

            verifyButton.isEnabled =
                false

            verifyButton.alpha =
                0.70f

            verifyButton.text =
                "Connecting to Solana..."

            solanaStatus.text =
                "Waiting for wallet..."

            lifecycleScope.launch {

                try {

                    val user =
                        auth.currentUser
                            ?: throw Exception(
                                "Please sign in first."
                            )

                    val result =
                        SolanaAchievementManager
                            .verifyAchievement(
                                achievementId =
                                    achievement.id,

                                achievementTitle =
                                    achievement.title
                            )

                    if (result.isSuccess) {

                        val verification =
                            result.getOrThrow()

                        db.collection("users")
                            .document(user.uid)
                            .collection("achievements")
                            .document(achievement.id)
                            .update(
                                mapOf(
                                    "solanaVerified" to true,
                                    "walletAddress" to
                                            verification.walletAddress,
                                    "transactionSignature" to
                                            verification.transactionSignature,
                                    "solanaNetwork" to
                                            "devnet",
                                    "solanaVerifiedAt" to
                                            System.currentTimeMillis()
                                )
                            )
                            .await()

                        solanaStatus.text =
                            "✓  VERIFIED ON SOLANA DEVNET\n" +
                                    shortenWalletAddress(
                                        verification.walletAddress
                                    )

                        solanaStatus.setTextColor(
                            Color.parseColor("#67D7A4")
                        )

                        verifyButton.text =
                            "✓  View on Solana Explorer"

                        verifyButton.isEnabled =
                            true

                        verifyButton.alpha =
                            1f

                        verifyButton.setOnClickListener {

                            openSolanaExplorer(
                                verification.transactionSignature
                            )
                        }

                        Toast.makeText(
                            this@AchievementsActivity,
                            "Achievement verified on Solana Devnet!",
                            Toast.LENGTH_LONG
                        ).show()

                    } else {

                        throw (
                                result.exceptionOrNull()
                                    ?: Exception(
                                        "Solana transaction failed."
                                    )
                                )
                    }

                } catch (e: Exception) {

                    e.printStackTrace()

                    verifyButton.isEnabled =
                        true

                    verifyButton.alpha =
                        1f

                    verifyButton.text =
                        "🔗  Verify with Solana Wallet"

                    solanaStatus.text =
                        "Verification failed."

                    solanaStatus.setTextColor(
                        Color.parseColor("#FF7D8A")
                    )

                    Toast.makeText(
                        this@AchievementsActivity,
                        e.message
                            ?: "Unable to verify achievement.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // --------------------------------------------------
    // SOLANA EXPLORER
    // --------------------------------------------------

    private fun openSolanaExplorer(
        transactionSignature: String
    ) {

        val explorerUrl =
            "https://explorer.solana.com/tx/" +
                    transactionSignature +
                    "?cluster=devnet"

        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(explorerUrl)
            )

        startActivity(intent)
    }

    // --------------------------------------------------
    // SHORTEN WALLET
    // --------------------------------------------------

    private fun shortenWalletAddress(
        address: String
    ): String {

        if (address.length <= 12) {
            return address
        }

        return address.take(6) +
                "..." +
                address.takeLast(6)
    }

    // --------------------------------------------------
    // DP
    // --------------------------------------------------

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }
}
