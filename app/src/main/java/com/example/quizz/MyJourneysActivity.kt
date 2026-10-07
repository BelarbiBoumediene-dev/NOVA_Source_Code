package com.example.quizz

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.quizz.data.remote.JsonParser
import com.example.quizz.data.remote.toJourney
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class MyJourneysActivity : AppCompatActivity() {

    private lateinit var journeysContainer: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var emptyState: LinearLayout
    private lateinit var createJourneyButton: MaterialButton

    // ============================================================
    // COLORS — MATCH MAIN QUEST UI
    // ============================================================

    private var cosmeticPalette =
        CosmeticThemeManager.paletteFor(
            FirebaseProgressManager.DEFAULT_COSMETIC
        )

    private val bgColor get() = cosmeticPalette.background
    private val cardColor get() = cosmeticPalette.surface
    private val cardStroke get() = cosmeticPalette.outline

    private val primaryBlue get() = cosmeticPalette.primary
    private val secondaryBlue get() = cosmeticPalette.primary

    private val purple get() = cosmeticPalette.secondary
    private val lightPurple get() = cosmeticPalette.secondary

    private val primaryText = Color.rgb(245, 247, 255)
    private val secondaryText = Color.rgb(137, 147, 174)
    private val mutedText = Color.rgb(98, 109, 137)

    // ============================================================
    // LIFECYCLE
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_my_journeys)
        refreshCosmeticTheme()

        setupSystemUi()
        setupViews()
        setupBottomNavigation()
        loadJourneys()
    }

    override fun onResume() {
        super.onResume()

        refreshCosmeticTheme()

        if (::journeysContainer.isInitialized) {
            loadJourneys()
        }
    }

    // ============================================================
    // SETUP
    // ============================================================

    private fun setupSystemUi() {

        window.statusBarColor = bgColor
        window.navigationBarColor = bgColor
        window.decorView.systemUiVisibility = 0
    }

    private fun refreshCosmeticTheme() {
        cosmeticPalette = CosmeticThemeManager.palette(this)
        CosmeticThemeManager.apply(this)

        if (::journeysContainer.isInitialized) {
            setupSystemUi()
            findViewById<MaterialButton>(R.id.createJourneyButton)?.let {
                CosmeticThemeManager.stylePrimaryAction(it)
            }
        }
    }

    private fun setupViews() {

        journeysContainer =
            findViewById(R.id.journeysContainer)

        emptyText =
            findViewById(R.id.emptyText)

        emptyState = findViewById(R.id.emptyState)
        createJourneyButton = findViewById(R.id.createJourneyButton)

        createJourneyButton.apply {

            CosmeticThemeManager.stylePrimaryAction(this)

            setTextColor(Color.WHITE)

            cornerRadius = 16.dp()

            stateListAnimator = null

            setOnClickListener {
                openCreateJourney()
            }
        }
    }

    // ============================================================
    // NAVIGATION
    // ============================================================

    private fun setupBottomNavigation() {

        findViewById<View>(R.id.navHome)
            .setOnClickListener { view ->

                animateNavigation(view)
                openHome()
            }

        findViewById<View>(R.id.navJourneys)
            .setOnClickListener { view ->

                animateNavigation(view)
            }

        findViewById<View>(R.id.navAchievements)
            .setOnClickListener { view ->

                animateNavigation(view)
                openAchievements()
            }

        findViewById<View>(R.id.navNova)
            .setOnClickListener { view ->

                animateNavigation(view)
                openLeaderboard()
            }
    }

    private fun openCreateJourney() {

        startActivity(
            Intent(
                this,
                MainActivity::class.java
            )
        )



    }

    private fun openHome() {

        val intent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        startActivity(intent)
    }

    private fun openAchievements() {

        startActivity(
            Intent(
                this,
                AchievementsActivity::class.java
            )
        )
    }

    private fun openLeaderboard() {

        startActivity(
            Intent(
                this,
                LeaderboardActivity::class.java
            )
        )
    }

    // ============================================================
    // NAVIGATION ANIMATION
    // ============================================================

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

    // ============================================================
    // LOAD JOURNEYS
    // ============================================================

    private fun loadJourneys() {

        val journeys =
            JourneyStorage.getJourneys(this)

        journeysContainer.removeAllViews()

        if (journeys.isEmpty()) {

            emptyText.visibility =
                View.VISIBLE

            emptyState.visibility =
                View.VISIBLE

            createJourneyButton.visibility =
                View.VISIBLE

            return
        }

        emptyText.visibility =
            View.GONE

        emptyState.visibility =
            View.GONE

        createJourneyButton.visibility =
            View.GONE

        journeys.forEachIndexed { index, journey ->

            val card =
                createJourneyCard(journey)

            journeysContainer.addView(card)

            animateCardEntrance(
                card,
                index
            )
        }
    }

    private fun animateCardEntrance(
        card: View,
        index: Int
    ) {

        card.alpha = 0f
        card.translationY = 24f
        card.scaleX = 0.98f
        card.scaleY = 0.98f

        card.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(index * 65L)
            .setDuration(330)
            .setInterpolator(
                DecelerateInterpolator()
            )
            .start()
    }

    // ============================================================
    // JOURNEY CARD
    // ============================================================

    private fun createJourneyCard(
        journey: SavedJourney
    ): LinearLayout {

        val card =
            createCardContainer()

        card.addView(
            createTopRow(journey)
        )

        card.addView(
            createGoal(journey)
        )

        card.addView(
            createDivider()
        )

        card.addView(
            createDetailsRow(journey)
        )

        val progress =
            calculateProgress(journey)

        card.addView(
            createProgressHeader(progress)
        )

        card.addView(
            createProgressBar(progress)
        )

        card.addView(
            createOpenButton(progress)
        )

        setupJourneyActions(
            card,
            journey
        )

        return card
    }

    // ============================================================
    // CARD
    // ============================================================

    private fun createCardContainer(): LinearLayout {

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    17.dp(),
                    17.dp(),
                    17.dp(),
                    17.dp()
                )

                background =
                    createCardBackground()

                isClickable = true
                isFocusable = true

                elevation = 3.dp().toFloat()

                stateListAnimator = null
            }

        card.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                setMargins(
                    0,
                    0,
                    0,
                    14.dp()
                )
            }

        return card
    }

    // ============================================================
    // TOP ROW
    // ============================================================

    private fun createTopRow(
        journey: SavedJourney
    ): LinearLayout {

        val row =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        row.addView(
            createQuestIcon()
        )

        row.addView(
            createTitleContainer(journey)
        )

        return row
    }

    private fun createQuestIcon(): TextView {

        return TextView(this).apply {

            text = "✦"

            textSize = 22f

            setTypeface(
                null,
                Typeface.BOLD
            )

            setTextColor(
                lightPurple
            )

            gravity =
                Gravity.CENTER

            background =
                GradientDrawable().apply {

                    cornerRadius =
                        16.dp().toFloat()

                    setColor(
                        Color.rgb(27, 25, 51)
                    )

                    setStroke(
                        1.dp(),
                        Color.rgb(74, 62, 125)
                    )
                }

            layoutParams =
                LinearLayout.LayoutParams(
                    50.dp(),
                    50.dp()
                )
        }
    }

    private fun createTitleContainer(
        journey: SavedJourney
    ): LinearLayout {

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {

                        setMargins(
                            13.dp(),
                            0,
                            8.dp(),
                            0
                        )
                    }
            }

        val title =
            TextView(this).apply {

                text =
                    journey.title

                textSize = 17f

                setTextColor(
                    primaryText
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                maxLines = 2

                ellipsize =
                    TextUtils.TruncateAt.END
            }

        val questLabel =
            TextView(this).apply {

                text =
                    "QUEST"

                textSize = 9f

                letterSpacing =
                    0.18f

                setTextColor(
                    lightPurple
                )

                setPadding(
                    0,
                    3.dp(),
                    0,
                    0
                )
            }

        container.addView(title)
        container.addView(questLabel)

        return container
    }

    // ============================================================
    // GOAL
    // ============================================================

    private fun createGoal(
        journey: SavedJourney
    ): TextView {

        return TextView(this).apply {

            text =
                "🎯  ${journey.goal}"

            textSize = 13f

            setTextColor(
                Color.rgb(157, 166, 188)
            )

            maxLines = 3

            ellipsize =
                TextUtils.TruncateAt.END

            setLineSpacing(
                1.4f,
                1f
            )

            setPadding(
                0,
                15.dp(),
                0,
                0
            )
        }
    }

    // ============================================================
    // DIVIDER
    // ============================================================

    private fun createDivider(): View {

        return View(this).apply {

            setBackgroundColor(
                Color.rgb(40, 52, 82)
            )

            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1.dp()
                ).apply {

                    setMargins(
                        0,
                        15.dp(),
                        0,
                        13.dp()
                    )
                }
        }
    }

    // ============================================================
    // DETAILS
    // ============================================================

    private fun createDetailsRow(
        journey: SavedJourney
    ): LinearLayout {

        val row =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        row.addView(
            createDetail(
                "⚔",
                journey.level
            )
        )

        row.addView(
            createDetail(
                "◷",
                journey.dailyTime
            )
        )

        row.addView(
            createDetail(
                "◈",
                journey.duration
            )
        )

        return row
    }

    private fun createDetail(
        icon: String,
        text: String
    ): TextView {

        return TextView(this).apply {

            this.text =
                "$icon  $text"

            textSize = 10f

            setTextColor(
                Color.rgb(137, 147, 174)
            )

            maxLines = 1

            ellipsize =
                TextUtils.TruncateAt.END

            gravity =
                Gravity.CENTER_VERTICAL

            layoutParams =
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {

                    setMargins(
                        0,
                        0,
                        5.dp(),
                        0
                    )
                }
        }
    }

    // ============================================================
    // PROGRESS
    // ============================================================

    private fun createProgressHeader(
        progress: Int
    ): LinearLayout {

        val row =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {

                        setMargins(
                            0,
                            19.dp(),
                            0,
                            8.dp()
                        )
                    }
            }

        val title =
            TextView(this).apply {

                text =
                    "QUEST PROGRESS"

                textSize = 9f

                letterSpacing =
                    0.15f

                setTextColor(
                    Color.rgb(120, 131, 159)
                )

                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
            }

        val value =
            TextView(this).apply {

                text =
                    "$progress%"

                textSize = 12f

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setTextColor(
                    if (progress >= 100) {
                        Color.rgb(103, 215, 164)
                    } else {
                        primaryText
                    }
                )
            }

        row.addView(title)
        row.addView(value)

        return row
    }

    private fun createProgressBar(
        progress: Int
    ): ProgressBar {

        return ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {

            max = 100

            this.progress =
                progress.coerceIn(0, 100)

            progressBackgroundTintList =
                colorStateList(
                    Color.rgb(32, 42, 66)
                )

            progressTintList =
                colorStateList(
                    if (progress >= 100) {
                        Color.rgb(103, 215, 164)
                    } else {
                        purple
                    }
                )

            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    5.dp()
                )
        }
    }

    private fun calculateProgress(
        journey: SavedJourney
    ): Int {

        return try {

            val response =
                JsonParser.parseJourney(
                    journey.journeyJson
                )

            val currentJourney =
                response.toJourney()

            val chapters =
                currentJourney.chapters

            if (chapters.isEmpty()) {
                return 0
            }

            val completedChapters =
                chapters.count { chapter ->

                    ProgressManager.isChapterCompleted(
                        this,
                        journey.id,
                        chapter.id
                    )
                }

            (completedChapters * 100) /
                    chapters.size

        } catch (e: Exception) {

            e.printStackTrace()

            0
        }
    }

    // ============================================================
    // OPEN BUTTON
    // ============================================================

    private fun createOpenButton(
        progress: Int
    ): MaterialButton {

        return MaterialButton(this).apply {

            text =
                if (progress >= 100) {
                    "✦   QUEST COMPLETED"
                } else {
                    "▶   CONTINUE QUEST"
                }

            textSize = 12f

            setTextColor(
                Color.WHITE
            )

            setTypeface(
                null,
                Typeface.BOLD
            )

            letterSpacing =
                0.06f

            backgroundTintList =
                if (progress >= 100) {
                    colorStateList(Color.rgb(38, 78, 69))
                } else {
                    CosmeticThemeManager.primaryButtonTint(primaryBlue)
                }

            cornerRadius =
                15.dp()

            stateListAnimator = null

            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    52.dp()
                ).apply {

                    setMargins(
                        0,
                        17.dp(),
                        0,
                        0
                    )
                }
        }
    }

    // ============================================================
    // JOURNEY ACTIONS
    // ============================================================

    private fun setupJourneyActions(
        card: LinearLayout,
        journey: SavedJourney
    ) {

        val openJourney = {

            card.animate()
                .scaleX(0.985f)
                .scaleY(0.985f)
                .setDuration(70)
                .withEndAction {

                    card.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()

                    openJourneyActivity(
                        journey
                    )
                }
                .start()
        }

        card.setOnClickListener {
            openJourney()
        }

        val openButton =
            card.getChildAt(
                card.childCount - 1
            )

        openButton.setOnClickListener {
            openJourney()
        }

        card.setOnLongClickListener {

            showDeleteDialog(
                journey
            )

            true
        }
    }

    private fun openJourneyActivity(
        journey: SavedJourney
    ) {

        val intent =
            Intent(
                this,
                JourneyActivity::class.java
            ).apply {

                putExtra(
                    "JOURNEY_ID",
                    journey.id
                )
            }

        startActivity(intent)
    }

    // ============================================================
    // CARD BACKGROUND
    // ============================================================

    private fun createCardBackground():
            GradientDrawable {

        return GradientDrawable().apply {

            cornerRadius =
                20.dp().toFloat()

            setColor(
                cardColor
            )

            setStroke(
                1.dp(),
                cardStroke
            )
        }
    }

    // ============================================================
    // DELETE DIALOG
    // ============================================================

    private fun showDeleteDialog(
        journey: SavedJourney
    ) {

        val dialog =
            Dialog(this)

        dialog.requestWindowFeature(
            Window.FEATURE_NO_TITLE
        )

        val container =
            createDeleteDialogContainer()

        container.addView(
            createDeleteIcon()
        )

        container.addView(
            createDeleteTitle()
        )

        container.addView(
            createDeleteQuestName(
                journey
            )
        )

        container.addView(
            createDeleteMessage()
        )

        val buttons =
            createDeleteButtons(
                dialog,
                journey
            )

        container.addView(buttons)

        dialog.setContentView(
            container
        )

        dialog.setCanceledOnTouchOutside(
            true
        )

        dialog.setOnShowListener {

            setupDeleteDialogWindow(
                dialog,
                container
            )
        }

        dialog.show()

        dialog.window?.let { window ->

            window.setBackgroundDrawableResource(
                android.R.color.transparent
            )

            window.setLayout(
                340.dp(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun createDeleteDialogContainer():
            LinearLayout {

        return LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            gravity =
                Gravity.CENTER_HORIZONTAL

            setPadding(
                24.dp(),
                24.dp(),
                24.dp(),
                21.dp()
            )

            background =
                GradientDrawable().apply {

                    cornerRadius =
                        24.dp().toFloat()

                    setColor(
                        Color.rgb(12, 17, 32)
                    )

                    setStroke(
                        1.dp(),
                        Color.rgb(48, 60, 90)
                    )
                }

            alpha = 0f
            scaleX = 0.92f
            scaleY = 0.92f
        }
    }

    private fun createDeleteIcon(): TextView {

        return TextView(this).apply {

            text =
                "×"

            textSize = 34f

            setTypeface(
                null,
                Typeface.BOLD
            )

            gravity =
                Gravity.CENTER

            setTextColor(
                Color.rgb(235, 116, 164)
            )

            background =
                GradientDrawable().apply {

                    shape =
                        GradientDrawable.OVAL

                    setColor(
                        Color.rgb(47, 25, 46)
                    )

                    setStroke(
                        1.dp(),
                        Color.rgb(126, 55, 103)
                    )
                }

            layoutParams =
                LinearLayout.LayoutParams(
                    62.dp(),
                    62.dp()
                )
        }
    }

    private fun createDeleteTitle(): TextView {

        return TextView(this).apply {

            text =
                "DELETE QUEST?"

            textSize = 19f

            setTextColor(
                primaryText
            )

            setTypeface(
                null,
                Typeface.BOLD
            )

            letterSpacing =
                0.08f

            gravity =
                Gravity.CENTER

            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {

                    setMargins(
                        0,
                        16.dp(),
                        0,
                        0
                    )
                }
        }
    }

    private fun createDeleteQuestName(
        journey: SavedJourney
    ): TextView {

        return TextView(this).apply {

            text =
                journey.title

            textSize = 14f

            setTextColor(
                lightPurple
            )

            setTypeface(
                null,
                Typeface.BOLD
            )

            gravity =
                Gravity.CENTER

            maxLines = 2

            ellipsize =
                TextUtils.TruncateAt.END

            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {

                    setMargins(
                        0,
                        7.dp(),
                        0,
                        0
                    )
                }
        }
    }

    private fun createDeleteMessage(): TextView {

        return TextView(this).apply {

            text =
                "This quest and its progress will be permanently removed."

            textSize = 12f

            setTextColor(
                Color.rgb(137, 147, 174)
            )

            gravity =
                Gravity.CENTER

            setPadding(
                8.dp(),
                10.dp(),
                8.dp(),
                0
            )

            setLineSpacing(
                2.dp().toFloat(),
                1.0f
            )
        }
    }

    private fun createDeleteButtons(
        dialog: Dialog,
        journey: SavedJourney
    ): LinearLayout {

        val buttons =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {

                        setMargins(
                            0,
                            22.dp(),
                            0,
                            0
                        )
                    }
            }

        val cancel =
            createCancelButton()

        val delete =
            createDeleteButton()

        cancel.setOnClickListener {
            dialog.dismiss()
        }

        delete.setOnClickListener {

            delete.isEnabled = false
            cancel.isEnabled = false

            delete.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(70)
                .withEndAction {
                    lifecycleScope.launch {
                        FirebaseProgressManager.deleteJourneyProgress(journey.id)
                            .onSuccess {
                                ProgressManager.clearJourney(this@MyJourneysActivity, journey.id)
                                JourneyStorage.deleteJourney(this@MyJourneysActivity, journey.id)
                                dialog.dismiss()
                                loadJourneys()
                            }
                            .onFailure { error ->
                                delete.isEnabled = true
                                cancel.isEnabled = true
                                android.widget.Toast.makeText(
                                    this@MyJourneysActivity,
                                    error.message ?: "Unable to delete cloud progress. Please try again.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                }
                .start()
        }

        buttons.addView(
            cancel,
            LinearLayout.LayoutParams(
                0,
                50.dp(),
                1f
            ).apply {

                setMargins(
                    0,
                    0,
                    6.dp(),
                    0
                )
            }
        )

        buttons.addView(
            delete,
            LinearLayout.LayoutParams(
                0,
                50.dp(),
                1f
            ).apply {

                setMargins(
                    6.dp(),
                    0,
                    0,
                    0
                )
            }
        )

        return buttons
    }

    private fun createCancelButton():
            MaterialButton {

        return MaterialButton(this).apply {

            text =
                "CANCEL"

            textSize = 11f

            setTextColor(
                Color.rgb(180, 183, 200)
            )

            setTypeface(
                null,
                Typeface.BOLD
            )

            backgroundTintList =
                colorStateList(
                    Color.rgb(28, 35, 55)
                )

            cornerRadius =
                14.dp()

            stateListAnimator = null
        }
    }

    private fun createDeleteButton():
            MaterialButton {

        return MaterialButton(this).apply {

            text =
                "DELETE QUEST"

            textSize = 11f

            setTextColor(
                Color.WHITE
            )

            setTypeface(
                null,
                Typeface.BOLD
            )

            backgroundTintList =
                colorStateList(
                    Color.rgb(128, 55, 105)
                )

            cornerRadius =
                14.dp()

            stateListAnimator = null
        }
    }

    private fun setupDeleteDialogWindow(
        dialog: Dialog,
        container: LinearLayout
    ) {

        val window =
            dialog.window ?: return

        window.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        window.addFlags(
            WindowManager.LayoutParams.FLAG_DIM_BEHIND
        )

        window.attributes =
            window.attributes.apply {

                dimAmount = 0.80f
            }

        window.setGravity(
            Gravity.CENTER
        )

        window.setLayout(
            340.dp(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        container.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(230)
            .setInterpolator(
                DecelerateInterpolator()
            )
            .start()
    }

    // ============================================================
    // UTILITIES
    // ============================================================

    private fun colorStateList(
        color: Int
    ): ColorStateList {

        return ColorStateList.valueOf(
            color
        )
    }

    private fun Int.dp(): Int {

        return (
                this *
                        resources.displayMetrics.density
                ).toInt()
    }
}
