package com.example.quizz

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var leaderboardList: LinearLayout

    private val db =
        FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_leaderboard
        )
        CosmeticThemeManager.apply(this)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0

        leaderboardList =
            findViewById(
                R.id.leaderboardList
            )

        // ------------------------------------------
        // BACK
        // ------------------------------------------

        findViewById<TextView>(
            R.id.backButton
        ).setOnClickListener {

            animateNavigation(it)

            finish()

            overridePendingTransition(
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        }

        // ------------------------------------------
        // WEEK LABEL
        // ------------------------------------------

        findViewById<TextView>(
            R.id.weeklyLabel
        ).text =
            "This week's top explorers"

        // ------------------------------------------
        // BOTTOM NAVIGATION
        // ------------------------------------------

        setupBottomNavigation()

        // ------------------------------------------
        // LOAD
        // ------------------------------------------

        loadLeaderboard()
    }

    // ==================================================
    // BOTTOM NAVIGATION
    // ==================================================

    private fun setupBottomNavigation() {

        // ------------------------------------------
        // HOME
        // ------------------------------------------

        findViewById<View>(
            R.id.navHome
        ).setOnClickListener { view ->

            animateNavigation(view)

            val intent =
                Intent(
                    this,
                    MainActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)


        }


        // ------------------------------------------
        // JOURNEYS
        // ------------------------------------------

        findViewById<View>(
            R.id.navJourneys
        ).setOnClickListener { view ->

            animateNavigation(view)

            val intent =
                Intent(
                    this,
                    MyJourneysActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)


        }


        // ------------------------------------------
        // TROPHIES
        // ------------------------------------------

        findViewById<View>(
            R.id.navAchievements
        ).setOnClickListener { view ->

            animateNavigation(view)

            val intent =
                Intent(
                    this,
                    AchievementsActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)


        }


        // ------------------------------------------
        // LEADERBOARD
        // ------------------------------------------

        findViewById<View>(
            R.id.navLeaderboard
        ).setOnClickListener { view ->

            animateNavigation(view)
        }
    }

    // ==================================================
    // NAVIGATION ANIMATION
    // ==================================================

    private fun animateNavigation(
        view: View
    ) {

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

    // ==================================================
    // LOAD LEADERBOARD
    // ==================================================

    private fun loadLeaderboard() {

        lifecycleScope.launch {

            try {

                val weekId =
                    FirebaseProgressManager
                        .currentWeekId()

                val snapshot =
                    db.collection("users")
                        .orderBy(
                            "weeklyXp",
                            Query.Direction.DESCENDING
                        )
                        .limit(100)
                        .get()
                        .await()

                val players =
                    snapshot.documents.filter {

                        it.getString(
                            "weeklyXpWeek"
                        ) == weekId
                    }

                // --------------------------------------
                // EMPTY
                // --------------------------------------

                if (players.isEmpty()) {

                    leaderboardList
                        .removeAllViews()

                    leaderboardList.addView(
                        emptyState()
                    )

                    return@launch
                }

                // --------------------------------------
                // DISPLAY
                // --------------------------------------

                leaderboardList
                    .removeAllViews()

                players.forEachIndexed { index, player ->

                    leaderboardList.addView(

                        playerRow(

                            rank =
                                index + 1,

                            name =
                                player.getString(
                                    "displayName"
                                ) ?: "Player",

                            xp =
                                player.getLong(
                                    "weeklyXp"
                                )?.toInt() ?: 0
                        )
                    )
                }

            } catch (e: Exception) {

                e.printStackTrace()

                leaderboardList
                    .removeAllViews()

                leaderboardList.addView(
                    errorState()
                )

                Toast.makeText(
                    this@LeaderboardActivity,
                    "Unable to load leaderboard.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ==================================================
    // PLAYER ROW
    // ==================================================

    private fun playerRow(
        rank: Int,
        name: String,
        xp: Int
    ): LinearLayout {

        return LinearLayout(this).apply {

            gravity =
                Gravity.CENTER_VERTICAL

            orientation =
                LinearLayout.HORIZONTAL

            setPadding(
                16.dp(),
                14.dp(),
                16.dp(),
                14.dp()
            )

            background =
                GradientDrawable().apply {

                    cornerRadius =
                        16.dp().toFloat()

                    setColor(
                        when (rank) {

                            1 ->
                                Color.rgb(
                                    37,
                                    31,
                                    58
                                )

                            2 ->
                                Color.rgb(
                                    30,
                                    32,
                                    51
                                )

                            3 ->
                                Color.rgb(
                                    28,
                                    30,
                                    48
                                )

                            else ->
                                Color.rgb(
                                    26,
                                    29,
                                    47
                                )
                        }
                    )

                    setStroke(
                        1.dp(),
                        when (rank) {

                            1 ->
                                Color.rgb(
                                    137,
                                    88,
                                    230
                                )

                            else ->
                                Color.rgb(
                                    42,
                                    46,
                                    67
                                )
                        }
                    )
                }


            // --------------------------------------
            // RANK
            // --------------------------------------

            addView(
                TextView(
                    this@LeaderboardActivity
                ).apply {

                    text =
                        when (rank) {

                            1 -> "👑  #1"

                            2 -> "🥈  #2"

                            3 -> "🥉  #3"

                            else -> "#$rank"
                        }

                    textSize =
                        if (rank <= 3) 15f
                        else 16f

                    setTextColor(
                        if (rank <= 3) {

                            Color.rgb(
                                169,
                                120,
                                255
                            )

                        } else {

                            Color.rgb(
                                169,
                                120,
                                255
                            )
                        }
                    )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )
                }
            )


            // --------------------------------------
            // NAME
            // --------------------------------------

            addView(
                TextView(
                    this@LeaderboardActivity
                ).apply {

                    text =
                        name

                    textSize =
                        16f

                    setTextColor(
                        Color.WHITE
                    )

                    setPadding(
                        16.dp(),
                        0,
                        8.dp(),
                        0
                    )

                    maxLines = 1

                    ellipsize =
                        android.text.TextUtils.TruncateAt.END

                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                }
            )


            // --------------------------------------
            // XP
            // --------------------------------------

            addView(
                TextView(
                    this@LeaderboardActivity
                ).apply {

                    text =
                        "$xp XP"

                    textSize =
                        14f

                    setTextColor(
                        Color.rgb(
                            139,
                            124,
                            255
                        )
                    )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )
                }
            )


            // --------------------------------------
            // MARGIN
            // --------------------------------------

            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {

                    bottomMargin =
                        10.dp()
                }
        }
    }

    // ==================================================
    // EMPTY STATE
    // ==================================================

    private fun emptyState(): TextView {

        return TextView(this).apply {

            text =
                "No scores this week yet.\nComplete a quiz to claim the first spot."

            textSize =
                15f

            gravity =
                Gravity.CENTER

            setTextColor(
                Color.rgb(
                    165,
                    168,
                    188
                )
            )

            setPadding(
                20.dp(),
                40.dp(),
                20.dp(),
                40.dp()
            )
        }
    }

    // ==================================================
    // ERROR STATE
    // ==================================================

    private fun errorState(): TextView {

        return TextView(this).apply {

            text =
                "Unable to load the leaderboard."

            textSize =
                15f

            gravity =
                Gravity.CENTER

            setTextColor(
                Color.rgb(
                    165,
                    168,
                    188
                )
            )

            setPadding(
                20.dp(),
                40.dp(),
                20.dp(),
                40.dp()
            )
        }
    }

    // ==================================================
    // DP
    // ==================================================

    private fun Int.dp(): Int {

        return (
                this *
                        resources.displayMetrics.density
                ).toInt()
    }
}
