package com.example.quizz

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.bumptech.glide.Glide

class NovaIntroActivity : AppCompatActivity() {

    private lateinit var messageContainer: LinearLayout
    private lateinit var messageScroll: ScrollView
    private lateinit var typingIndicator: LinearLayout
    private lateinit var answerContainer: LinearLayout

    private lateinit var yesButton: MaterialButton
    private lateinit var noButton: MaterialButton

    private val handler = Handler(Looper.getMainLooper())

    private val user = FirebaseAuth.getInstance().currentUser
    private val fullName = user?.displayName ?: ""


    private val lastName = fullName.trim().substringAfterLast(" ")



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        if (prefs.getBoolean("nova_intro_completed", false)) {

            val intent = Intent(
                this,
                MainActivity::class.java
            )

            startActivity(intent)
            finish()

            return
        }

        setContentView(R.layout.activity_nova_intro)

        val novaCharacter =
            findViewById<com.google.android.material.imageview.ShapeableImageView>(
                R.id.novaCharacter
            )

        Glide.with(this)
            .asGif()
            .load(R.drawable.msgif)
            .into(novaCharacter)




        initializeViews()
        startNovaAnimation()
        startConversation()
    }


    // =========================================================
    // INITIALIZE
    // =========================================================

    private fun initializeViews() {

        messageContainer = findViewById(R.id.messageContainer)
        messageScroll = findViewById(R.id.messageScroll)

        typingIndicator = findViewById(R.id.typingIndicator)

        answerContainer = findViewById(R.id.answerContainer)

        yesButton = findViewById(R.id.yesButton)
        noButton = findViewById(R.id.noButton)

        yesButton.setOnClickListener {
            answerSelected(true)
        }

        noButton.setOnClickListener {
            answerSelected(false)
        }
    }

    // =========================================================
    // NOVA CHARACTER ANIMATION
    // =========================================================

    private fun startNovaAnimation() {

        val nova = findViewById<View>(R.id.novaCharacter)

        val animation = AnimationUtils.loadAnimation(
            this,
            R.anim.nova_enter
        )

        nova.startAnimation(animation)

        startFloatingLoop(nova)
    }

    private fun startFloatingLoop(view: View) {

        view.animate()
            .translationY(-6f)
            .setDuration(1400)
            .withEndAction {

                view.animate()
                    .translationY(0f)
                    .setDuration(1400)
                    .withEndAction {

                        if (!isFinishing) {
                            startFloatingLoop(view)
                        }

                    }
                    .start()
            }
            .start()
    }

    // =========================================================
    // CONVERSATION
    // =========================================================

    private fun startConversation() {

        // -------------------------
        // MESSAGE 1
        // -------------------------

        handler.postDelayed({

            showTyping()

        }, 500)

        handler.postDelayed({

            hideTyping()

            addNovaMessage(
                "Welcome $lastName 👋"
            )

        }, 1500)


        // -------------------------
        // MESSAGE 2
        // -------------------------

        handler.postDelayed({

            showTyping()

        }, 2500)

        handler.postDelayed({

            hideTyping()

            addNovaMessage(
                "I'm Nova ✦"
            )

        }, 3500)


        // -------------------------
        // MESSAGE 3
        // -------------------------

        handler.postDelayed({

            showTyping()

        }, 4500)

        handler.postDelayed({

            hideTyping()

            addNovaMessage(
                "I'll be your companion on this journey, " +
                        "and I'll help you discover what awaits you here."
            )

        }, 5700)


        // -------------------------
        // QUESTION
        // -------------------------

        handler.postDelayed({

            showTyping()

        }, 7000)

        handler.postDelayed({

            hideTyping()

            addNovaMessage(
                "Are you ready to start the game? 🎮"
            )

        }, 8200)


        // -------------------------
        // SHOW ANSWERS
        // -------------------------

        handler.postDelayed({

            showAnswers()

        }, 8500)
    }

    // =========================================================
    // TYPING
    // =========================================================

    private fun showTyping() {

        typingIndicator.visibility = View.VISIBLE
        typingIndicator.alpha = 0f

        typingIndicator.animate()
            .alpha(1f)
            .setDuration(180)
            .start()

        animateTypingDots()

        scrollToBottom()
    }

    private fun hideTyping() {

        typingIndicator.animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction {

                typingIndicator.visibility = View.GONE

            }
            .start()
    }

    private fun animateTypingDots() {

        val dots = findViewById<TextView>(R.id.typingDots)

        dots.animate()
            .alpha(0.35f)
            .setDuration(450)
            .withEndAction {

                dots.animate()
                    .alpha(1f)
                    .setDuration(450)
                    .withEndAction {

                        if (typingIndicator.visibility == View.VISIBLE) {
                            animateTypingDots()
                        }

                    }
                    .start()

            }
            .start()
    }

    // =========================================================
    // NOVA MESSAGE
    // =========================================================

    private fun addNovaMessage(message: String) {

        val textView = TextView(this)

        textView.text = message

        textView.textSize = 16f

        textView.setTextColor(Color.WHITE)

        textView.textDirection =
            View.TEXT_DIRECTION_RTL

        textView.gravity =
            Gravity.RIGHT

        textView.background =
            getDrawable(R.drawable.nova_message)

        textView.setPadding(
            18,
            13,
            18,
            13
        )

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.gravity = Gravity.RIGHT
        params.bottomMargin = 12

        textView.layoutParams = params

        messageContainer.addView(textView)

        val animation = AnimationUtils.loadAnimation(
            this,
            R.anim.message_in
        )

        textView.startAnimation(animation)

        scrollToBottom()
    }

    // =========================================================
    // USER MESSAGE
    // =========================================================

    private fun addUserMessage(message: String) {

        val textView = TextView(this)

        textView.text = message

        textView.textSize = 15f

        textView.setTextColor(Color.WHITE)

        textView.textDirection =
            View.TEXT_DIRECTION_RTL

        textView.gravity =
            Gravity.CENTER

        textView.background =
            getDrawable(R.drawable.user_message)

        textView.setPadding(
            18,
            11,
            18,
            11
        )

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // USER = LEFT
        params.gravity = Gravity.LEFT

        params.bottomMargin = 12

        textView.layoutParams = params

        messageContainer.addView(textView)

        val animation = AnimationUtils.loadAnimation(
            this,
            R.anim.message_in
        )

        textView.startAnimation(animation)

        scrollToBottom()
    }

    // =========================================================
    // ANSWERS
    // =========================================================

    private fun showAnswers() {

        answerContainer.visibility = View.VISIBLE
        answerContainer.alpha = 0f
        answerContainer.translationX = -25f

        answerContainer.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(350)
            .start()

        scrollToBottom()
    }

    // =========================================================
    // ANSWER SELECTED
    // =========================================================

    private fun answerSelected(yes: Boolean) {

        yesButton.isEnabled = false
        noButton.isEnabled = false

        val answer =
            if (yes) {
                "Yes ✦"
            } else {
                "No"
            }

        // Hide answer buttons
        answerContainer.animate()
            .alpha(0f)
            .translationX(-20f)
            .setDuration(200)
            .withEndAction {

                answerContainer.visibility = View.GONE

                // Add selected answer as USER message
                addUserMessage(answer)

                continueConversation(yes)
            }
            .start()
    }

    // =========================================================
    // NOVA RESPONSE
    // =========================================================

    private fun continueConversation(yes: Boolean) {

        if (yes) {

            // -------------------------
            // YES
            // -------------------------

            handler.postDelayed({

                showTyping()

            }, 400)

            handler.postDelayed({

                hideTyping()

                addNovaMessage(
                    "I knew you were ready 😎"
                )

            }, 1300)


            handler.postDelayed({

                showTyping()

            }, 2200)

            handler.postDelayed({

                hideTyping()

                addNovaMessage(
                    "Then... let the adventure begin ✦"
                )

            }, 3200)


            handler.postDelayed({

                showTyping()

            }, 4100)

            handler.postDelayed({

                hideTyping()

                addNovaMessage(
                    "I hope you have an amazing experience, $lastName 🎮"
                )

            }, 5200)


            handler.postDelayed({

                openMainActivity()

            }, 7000)

        } else {

            // -------------------------
            // NO
            // -------------------------

            handler.postDelayed({

                showTyping()

            }, 400)

            handler.postDelayed({

                hideTyping()

                addNovaMessage(
                    "That's okay..."
                )

            }, 1300)


            handler.postDelayed({

                showTyping()

            }, 2100)

            handler.postDelayed({

                hideTyping()

                addNovaMessage(
                    "Maybe you'll change your mind once you see " +
                            "what the game has in store for you 😉"
                )

            }, 3300)


            handler.postDelayed({

                showTyping()

            }, 4300)

            handler.postDelayed({

                hideTyping()

                addNovaMessage(
                    "Either way, I hope you have an amazing experience ✦"
                )

            }, 5400)


            handler.postDelayed({

                openMainActivity()

            }, 7200)
        }
    }

    // =========================================================
    // AUTO SCROLL
    // =========================================================

    private fun scrollToBottom() {

        messageScroll.post {

            messageScroll.fullScroll(
                View.FOCUS_DOWN
            )
        }
    }

    // =========================================================
    // OPEN MAIN ACTIVITY
    // =========================================================


    private fun openMainActivity() {

        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("nova_intro_completed", true)
            .apply()

        val intent = Intent(
            this,
            MainActivity::class.java
        )

        startActivity(intent)

        overridePendingTransition(0, 0)

        finish()
    }



    // =========================================================
    // CLEAN UP
    // =========================================================

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)

        super.onDestroy()
    }
}
