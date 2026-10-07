package com.example.quizz

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class WelcomeActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_NOVA_COMPLETED = "nova_intro_completed"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0

        authManager = AuthManager(this)

        /*
         * -----------------------------------------
         * CHECK CURRENT USER
         * -----------------------------------------
         */

        val currentUser = authManager.getCurrentUser()

        if (currentUser != null) {

            /*
             * User is already signed in.
             * Now check if Nova Intro was completed.
             */

            val prefs = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
            )

            val novaCompleted =
                prefs.getBoolean(
                    KEY_NOVA_COMPLETED,
                    false
                )

            if (novaCompleted) {

                openMainActivity()

            } else {

                openNovaIntro()

            }

            return
        }

        /*
         * -----------------------------------------
         * SHOW WELCOME / GOOGLE SIGN-IN
         * -----------------------------------------
         */

        setContentView(R.layout.activity_welcome)

        val continueButton =
            findViewById<MaterialButton>(
                R.id.continueButton
            )

        continueButton.setOnClickListener {

            continueButton.isEnabled = false
            continueButton.text = "CONNECTING..."

            lifecycleScope.launch {

                val result =
                    authManager.signInWithGoogle()

                if (result.isSuccess) {

                    /*
                     * Google Sign-In successful.
                     *
                     * First login goes to Nova.
                     */

                    openNovaIntro()

                } else {

                    continueButton.isEnabled = true
                    continueButton.text =
                        "CONTINUE WITH GOOGLE"

                    val error =
                        result.exceptionOrNull()

                    Toast.makeText(
                        this@WelcomeActivity,
                        error?.message
                            ?: "Google Sign-In failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /*
     * -----------------------------------------
     * NOVA INTRO
     * -----------------------------------------
     */

    private fun openNovaIntro() {

        val intent =
            Intent(
                this,
                NovaIntroActivity::class.java
            )

        startActivity(intent)
        finish()
    }

    /*
     * -----------------------------------------
     * MAIN ACTIVITY
     * -----------------------------------------
     */

    private fun openMainActivity() {

        val intent =
            Intent(
                this,
                MainActivity::class.java
            )

        startActivity(intent)
        finish()
    }
}