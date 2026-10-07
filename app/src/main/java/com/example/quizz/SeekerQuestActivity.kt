package com.example.quizz

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class SeekerQuestActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var walletText: TextView
    private lateinit var actionButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seeker_quest)
        CosmeticThemeManager.apply(this)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0

        SolanaAchievementManager.initialize(this)

        statusText = findViewById(R.id.seekerQuestStatus)
        walletText = findViewById(R.id.seekerWalletText)
        actionButton = findViewById(R.id.seekerQuestAction)

        findViewById<TextView>(R.id.seekerQuestBack).setOnClickListener { finish() }
        actionButton.setOnClickListener { verifyWallet() }
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            FirebaseProgressManager.getSeekerQuestProfile()
                .onSuccess(::renderProfile)
                .onFailure {
                    statusText.text = "Sign in to access Seeker quests."
                    actionButton.isEnabled = false
                }
        }
    }

    private fun renderProfile(profile: FirebaseProgressManager.SeekerQuestProfile) {
        if (!profile.isVerified) {
            statusText.text = "Verify your wallet once to unlock a daily quest and a 5 Coin reward."
            walletText.text = "Wallet not connected"
            actionButton.text = "VERIFY WITH SOLANA WALLET"
            actionButton.isEnabled = true
            return
        }

        walletText.text = profile.walletAddress?.let(::shortenAddress) ?: "Verified wallet"
        actionButton.isEnabled = false
        actionButton.text = if (profile.completedToday) {
            "DAILY QUEST COMPLETE"
        } else {
            "COMPLETE ANY QUIZ TODAY"
        }
        statusText.text = if (profile.completedToday) {
            "You claimed today's Seeker reward. Return tomorrow for a new quest."
        } else {
            "Today's quest: pass any lesson quiz to earn 5 Coins."
        }
    }

    private fun verifyWallet() {
        actionButton.isEnabled = false
        actionButton.text = "CONNECTING TO WALLET..."

        lifecycleScope.launch {
            val verification = SolanaAchievementManager.verifyAchievement(
                achievementId = "seeker_identity",
                achievementTitle = "Seeker Explorer"
            )

            verification.onSuccess { result ->
                FirebaseProgressManager.saveSeekerVerification(
                    walletAddress = result.walletAddress,
                    transactionSignature = result.transactionSignature
                ).onSuccess {
                    Toast.makeText(
                        this@SeekerQuestActivity,
                        "Seeker daily quests unlocked!",
                        Toast.LENGTH_LONG
                    ).show()
                    loadProfile()
                }.onFailure {
                    restoreVerificationAction(it.message)
                }
            }.onFailure {
                restoreVerificationAction(it.message)
            }
        }
    }

    private fun restoreVerificationAction(message: String?) {
        actionButton.isEnabled = true
        actionButton.text = "VERIFY WITH SOLANA WALLET"
        Toast.makeText(
            this,
            message ?: "Unable to verify the wallet.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun shortenAddress(address: String): String {
        return if (address.length <= 12) address else "${address.take(6)}...${address.takeLast(6)}"
    }
}
