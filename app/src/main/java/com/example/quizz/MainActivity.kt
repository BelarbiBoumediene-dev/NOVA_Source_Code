package com.example.quizz

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope

import com.example.quizz.databinding.ActivityMainBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private companion object {
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        const val PREFS_NAME = "app_prefs"
        const val KEY_SEEKER_PROMPT_SEEN_PREFIX = "seeker_prompt_seen_"
    }

    private lateinit var binding: ActivityMainBinding

    private var selectedLevel = "Beginner"
    private var selectedDailyTime = "30 min / day"
    private var selectedDuration = "7 days"

    private lateinit var userNameText: TextView
    private lateinit var levelText: TextView
    private lateinit var xpText: TextView

    private lateinit var coinsText: TextView
    private lateinit var xpProgressBar: ProgressBar
    private lateinit var playerAvatar: ImageView
    private lateinit var playerAvatarEffect: ImageView
    private var seekerAttentionAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        CosmeticThemeManager.apply(this)

        // =========================================================
        // FIND VIEWS
        // =========================================================

        userNameText = findViewById(R.id.userNameText)
        levelText = findViewById(R.id.levelText)
        xpText = findViewById(R.id.xpText)
        coinsText = findViewById(R.id.coinsText)
        xpProgressBar = findViewById(R.id.xpProgressBar)
        playerAvatar = findViewById(R.id.playerAvatar)
        playerAvatarEffect = findViewById(R.id.playerAvatarEffect)

        // =========================================================
        // SYSTEM UI
        // =========================================================

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0

        // =========================================================
        // FIREBASE USER
        // =========================================================

        val user = FirebaseAuth.getInstance().currentUser

        val fullName = user?.displayName?.trim().orEmpty()

        val db = FirebaseFirestore.getInstance()

        if (user != null) {
            db.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->

                    val coins = document.getLong("Coins")?.toInt() ?: 0

                    val lastName = if (fullName.isNotEmpty()) {
                        fullName.substringAfterLast(" ").trim()
                    } else {
                        ""
                    }

                    //coinsTextView.text = coins.toString()
                    coinsText.text = "◈  ${coins.toString()}"


                    binding.userNameText.text =
                        if (lastName.isNotEmpty()) {
                            lastName.uppercase()
                        } else {
                            "NOVA"
                        }



                }
        }


        // =========================================================
        // PLAYER AVATAR
        // =========================================================

        playerAvatar.setOnClickListener {
            showLevelAvatarDialog()
        }

        // =========================================================
        // LOGOUT
        // =========================================================

        binding.logoutButton.setOnClickListener {

            binding.logoutButton.animate()
                .scaleX(0.88f)
                .scaleY(0.88f)
                .setDuration(70)
                .withEndAction {

                    binding.logoutButton.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .withEndAction {

                            showLogoutDialog()

                        }
                        .start()
                }
                .start()
        }

        // =========================================================
        // INITIAL USER DATA
        // =========================================================

        loadUserStats()

        // =========================================================
        // BOTTOM NAVIGATION
        // =========================================================

        setupBottomNavigation()

        // =========================================================
        // OTHER BUTTONS
        // =========================================================

        setupClickListeners()

        // =========================================================
        // NOTIFICATION PERMISSION
        // =========================================================

        requestNotificationPermission()
    }

    // =============================================================
    // REFRESH USER DATA WHEN RETURNING TO MAIN SCREEN
    // =============================================================

    override fun onResume() {
        super.onResume()

        if (::userNameText.isInitialized) {
            loadUserStats()
            loadSeekerQuestStatus()
        }
    }

    // =============================================================
    // LOAD USER STATS
    // =============================================================


    private fun loadUserStats() {

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

                val displayName =
                    document.getString("displayName")
                        ?: "NOVA"

                val level =
                    document.getLong("TotaleLevel")
                        ?.toInt()
                        ?: 1

                val xp =
                    document.getLong("TotaleXp")
                        ?.toInt()
                        ?: 0

                // -------------------------------------------------
                // USER NAME
                // -------------------------------------------------

                userNameText.text =
                    displayName.uppercase()

                // -------------------------------------------------
                // LEVEL
                // -------------------------------------------------

                levelText.text =
                    "LEVEL $level"

                // -------------------------------------------------
                // XP
                // -------------------------------------------------

                xpText.text =
                    "$xp XP"

                // -------------------------------------------------
                // XP BAR
                // -------------------------------------------------

                updateXpBar(
                    level = level,
                    totalXp = xp
                )

                // -------------------------------------------------
                // AVATAR
                // -------------------------------------------------

                val cosmeticId = document.getString("selectedCosmetic")
                    ?: FirebaseProgressManager.DEFAULT_COSMETIC

                CosmeticThemeManager.cacheSelectedCosmetic(this, cosmeticId)
                CosmeticThemeManager.apply(this)
                updatePlayerAvatar(level, cosmeticId)
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Unable to load player data",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // =============================================================
    // DAILY SEEKER QUEST
    // =============================================================

    private fun loadSeekerQuestStatus() {

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            hideSeekerAttentionBadge()
            return
        }

        lifecycleScope.launch {

            FirebaseProgressManager
                .getSeekerQuestProfile()
                .onSuccess { profile ->

                    binding.seekerQuestStatus.text =
                        when {
                            !profile.isVerified ->
                                "CONNECT WALLET · UNLOCK DAILY QUEST"

                            profile.completedToday ->
                                "REWARD CLAIMED TODAY  ✓"

                            else ->
                                "PASS ANY QUIZ  •  +5 ◈"
                        }

                    if (profile.isVerified) {
                        hideSeekerAttentionBadge()
                    } else {
                        showSeekerAttentionBadge(uid)
                    }
                }
                .onFailure {

                    // Keep the card useful even if the profile cannot load.
                    binding.seekerQuestStatus.text =
                        "DAILY QUEST  •  +5 ◈"
                    hideSeekerAttentionBadge()
                }
        }
    }

    private fun showSeekerAttentionBadge(uid: String) {

        val hasSeenPrompt = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getBoolean(KEY_SEEKER_PROMPT_SEEN_PREFIX + uid, false)

        if (hasSeenPrompt || binding.seekerQuestAttentionBadge.visibility == View.VISIBLE) {
            return
        }

        binding.seekerQuestAttentionBadge.visibility = View.VISIBLE
        binding.seekerQuestAttentionBadge.alpha = 1f

        seekerAttentionAnimator?.cancel()
        seekerAttentionAnimator = ObjectAnimator.ofFloat(
            binding.seekerQuestAttentionBadge,
            View.ALPHA,
            1f,
            0.42f
        ).also { animator ->
            animator.apply {
            duration = 850
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
            }
        }
    }

    private fun dismissSeekerAttentionBadge() {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SEEKER_PROMPT_SEEN_PREFIX + uid, true)
            .apply()

        hideSeekerAttentionBadge()
    }

    private fun hideSeekerAttentionBadge() {

        seekerAttentionAnimator?.cancel()
        seekerAttentionAnimator = null
        binding.seekerQuestAttentionBadge.visibility = View.GONE
        binding.seekerQuestAttentionBadge.alpha = 1f
    }

    // =============================================================
    // UPDATE XP BAR
    // =============================================================

    private fun updateXpBar(
        level: Int,
        totalXp: Int
    ) {

        val xpPerLevel = 100

        val currentLevelXp =
            totalXp % xpPerLevel

        xpProgressBar.max =
            xpPerLevel

        xpProgressBar.progress =
            currentLevelXp
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
        playerAvatarEffect.setImageDrawable(null)
        playerAvatarEffect.visibility = View.GONE
    }

    // =============================================================
    // AVATAR LEVELS DIALOG
    // =============================================================



    private fun showLevelAvatarDialog() {

        val dialog = Dialog(this)

        dialog.requestWindowFeature(
            Window.FEATURE_NO_TITLE
        )

        // =========================================================
        // MAIN ROOT
        // =========================================================

        val root = LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                dp(20),
                dp(20),
                dp(20),
                dp(16)
            )

            background =
                GradientDrawable().apply {

                    cornerRadius =
                        dp(24).toFloat()

                    setColor(
                        Color.rgb(
                            9,
                            14,
                            29
                        )
                    )

                    setStroke(
                        dp(1),
                        Color.rgb(
                            100,
                            75,
                            190
                        )
                    )
                }
        }

        // =========================================================
        // TITLE
        // =========================================================

        val title =
            TextView(this).apply {

                text = "YOUR AVATARS"

                setTextColor(
                    Color.WHITE
                )

                textSize = 20f

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                letterSpacing =
                    0.08f

                gravity =
                    Gravity.CENTER
            }

        root.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // =========================================================
        // SUBTITLE
        // =========================================================

        val subtitle =
            TextView(this).apply {

                text =
                    "Level up to unlock new avatars"

                setTextColor(
                    Color.rgb(
                        145,
                        150,
                        175
                    )
                )

                textSize = 11f

                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    dp(6),
                    0,
                    dp(14)
                )
            }

        root.addView(
            subtitle
        )

        // =========================================================
        // CURRENT LEVEL
        // =========================================================

        val currentLevel =
            getCurrentLevelFromText()

        // =========================================================
        // SCROLL VIEW
        // =========================================================

        val scrollView =
            androidx.core.widget.NestedScrollView(this).apply {

                isFillViewport = false

                overScrollMode =
                    View.OVER_SCROLL_IF_CONTENT_SCROLLS

                isVerticalScrollBarEnabled = true

                // scrollbarFadeDuration = 300
            }

        // =========================================================
        // AVATAR LIST
        // =========================================================

        val avatarContainer =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    0,
                    0,
                    dp(2),
                    0
                )
            }

        // =========================================================
        // AVATAR DATA
        // =========================================================

        val cosmeticId = CosmeticThemeManager.selectedCosmetic(this)
        val avatars = listOf(1, 5, 10, 20, 35, 50, 75, 100).map { level ->
            AvatarInfo(
                level = level,
                image = CosmeticThemeManager.avatarForLevel(level, cosmeticId)
            )
        }

        // =========================================================
        // CREATE AVATAR ROWS
        // =========================================================

        avatars.forEachIndexed { index, avatar ->

            val unlocked =
                currentLevel >= avatar.level

            val row =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.HORIZONTAL

                    gravity =
                        Gravity.CENTER_VERTICAL

                    setPadding(
                        dp(10),
                        dp(8),
                        dp(10),
                        dp(8)
                    )

                    background =
                        GradientDrawable().apply {

                            cornerRadius =
                                dp(14).toFloat()

                            if (unlocked) {

                                setColor(
                                    Color.rgb(
                                        22,
                                        27,
                                        48
                                    )
                                )

                                setStroke(
                                    dp(1),
                                    Color.rgb(
                                        70,
                                        60,
                                        110
                                    )
                                )

                            } else {

                                setColor(
                                    Color.rgb(
                                        14,
                                        18,
                                        32
                                    )
                                )

                                setStroke(
                                    dp(1),
                                    Color.rgb(
                                        38,
                                        42,
                                        60
                                    )
                                )
                            }
                        }

                    alpha =
                        if (unlocked) {
                            1f
                        } else {
                            0.42f
                        }
                }

            // =====================================================
            // AVATAR IMAGE
            // =====================================================

            val avatarImage =
                ImageView(this).apply {

                    setImageResource(
                        avatar.image
                    )

                    scaleType =
                        ImageView.ScaleType.CENTER_CROP

                    contentDescription =
                        "Avatar ${index + 1}"

                    alpha =
                        if (unlocked) {
                            1f
                        } else {
                            0.55f
                        }
                }

            row.addView(
                avatarImage,
                LinearLayout.LayoutParams(
                    dp(58),
                    dp(58)
                )
            )

            // =====================================================
            // TEXT CONTAINER
            // =====================================================

            val textContainer =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    gravity =
                        Gravity.CENTER_VERTICAL

                    setPadding(
                        dp(14),
                        0,
                        dp(8),
                        0
                    )

                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                }

            val avatarName =
                TextView(this).apply {

                    text =
                        "AVATAR ${index + 1}"

                    setTextColor(
                        Color.WHITE
                    )

                    textSize = 13f

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    letterSpacing =
                        0.05f
                }

            val unlockText =
                TextView(this).apply {

                    text =
                        if (unlocked) {
                            "Unlocked at level ${avatar.level}"
                        } else {
                            "Unlock at level ${avatar.level}"
                        }

                    setTextColor(
                        if (unlocked) {
                            Color.rgb(
                                170,
                                150,
                                255
                            )
                        } else {
                            Color.rgb(
                                120,
                                125,
                                145
                            )
                        }
                    )

                    textSize = 10f

                    setPadding(
                        0,
                        dp(4),
                        0,
                        0
                    )
                }

            textContainer.addView(
                avatarName
            )

            textContainer.addView(
                unlockText
            )

            row.addView(
                textContainer
            )

            // =====================================================
            // STATUS
            // =====================================================

            val status =
                TextView(this).apply {

                    text =
                        if (unlocked) {
                            "UNLOCKED"
                        } else {
                            "LOCKED"
                        }

                    setTextColor(
                        if (unlocked) {
                            Color.rgb(
                                105,
                                220,
                                170
                            )
                        } else {
                            Color.rgb(
                                100,
                                105,
                                125
                            )
                        }
                    )

                    textSize = 8f

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    letterSpacing =
                        0.08f

                    gravity =
                        Gravity.CENTER
                }

            row.addView(
                status,
                LinearLayout.LayoutParams(
                    dp(70),
                    dp(32)
                )
            )

            // =====================================================
            // ROW
            // =====================================================

            val rowParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(76)
                )

            rowParams.bottomMargin =
                dp(8)

            avatarContainer.addView(
                row,
                rowParams
            )
        }

        // =========================================================
        // PUT AVATAR LIST INSIDE SCROLL VIEW
        // =========================================================

        scrollView.addView(
            avatarContainer,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // =========================================================
        // SCROLL VIEW HEIGHT
        // =========================================================

        val scrollParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(430)
            )

        root.addView(
            scrollView,
            scrollParams
        )

        // =========================================================
        // CLOSE BUTTON
        // =========================================================

        val close =
            TextView(this).apply {

                text = "CLOSE"

                setTextColor(
                    Color.rgb(
                        165,
                        135,
                        255
                    )
                )

                textSize = 11f

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                letterSpacing =
                    0.14f

                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    dp(8),
                    0,
                    0
                )

                setOnClickListener {

                    dialog.dismiss()
                }
            }

        root.addView(
            close,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(42)
            )
        )

        // =========================================================
        // SET DIALOG CONTENT
        // =========================================================

        dialog.setContentView(
            root
        )

        dialog.setCanceledOnTouchOutside(
            true
        )

        // =========================================================
        // SHOW DIALOG
        // =========================================================

        dialog.setOnShowListener {

            val window =
                dialog.window
                    ?: return@setOnShowListener

            window.setBackgroundDrawable(
                ColorDrawable(
                    Color.TRANSPARENT
                )
            )

            window.setGravity(
                Gravity.CENTER
            )

            window.setDimAmount(
                0.78f
            )

            window.addFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND
            )

            window.setLayout(
                (resources.displayMetrics.widthPixels * 0.90f)
                    .toInt(),
                dp(560)
            )
        }

        dialog.show()

        // =========================================================
        // FINAL WINDOW SIZE
        // =========================================================

        dialog.window?.let { window ->

            window.setBackgroundDrawable(
                ColorDrawable(
                    Color.TRANSPARENT
                )
            )

            window.setGravity(
                Gravity.CENTER
            )

            window.setLayout(
                (resources.displayMetrics.widthPixels * 0.90f)
                    .toInt(),
                dp(560)
            )
        }
    }


    // =============================================================
    // GET CURRENT LEVEL
    // =============================================================

    private fun getCurrentLevelFromText(): Int {

        val text =
            levelText.text
                ?.toString()
                .orEmpty()

        return text
            .removePrefix("LEVEL")
            .trim()
            .toIntOrNull()
            ?: 1
    }

    // =============================================================
    // AVATAR DATA CLASS
    // =============================================================

    private data class AvatarInfo(
        val level: Int,
        val image: Int
    )

    // =============================================================
    // BOTTOM NAVIGATION
    // =============================================================

    private fun setupBottomNavigation() {

        // HOME
        binding.navHome.setOnClickListener {

            animateNavClick(
                binding.navHome
            )

            // Already on Home
        }

        // JOURNEYS
        binding.navJourneys.setOnClickListener {

            animateNavClick(
                binding.navJourneys
            )

            startActivityWithAnimation(
                Intent(
                    this,
                    MyJourneysActivity::class.java
                )
            )
        }

        // TROPHIES
        binding.navAchievements.setOnClickListener {

            animateNavClick(
                binding.navAchievements
            )

            startActivityWithAnimation(
                Intent(
                    this,
                    AchievementsActivity::class.java
                )
            )
        }

        // NOVA
        binding.navNova.setOnClickListener {

            animateNavClick(
                binding.navNova
            )

            startActivityWithAnimation(
                Intent(
                    this,
                    LeaderboardActivity::class.java
                )
            )
        }
    }

    // =============================================================
    // NAVIGATION ANIMATION
    // =============================================================

    private fun animateNavClick(
        view: View
    ) {

        view.animate()
            .scaleX(0.90f)
            .scaleY(0.90f)
            .setDuration(70)
            .withEndAction {

                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }

    private fun startActivityWithAnimation(
        intent: Intent
    ) {

        startActivity(intent)
    }

    // =============================================================
    // NOTIFICATION PERMISSION
    // =============================================================

    private fun requestNotificationPermission() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    // =============================================================
    // LOGOUT DIALOG
    // =============================================================

    private fun showLogoutDialog() {

        val dialog =
            Dialog(this)

        dialog.setContentView(
            R.layout.dialog_logout
        )

        dialog.window?.setBackgroundDrawable(
            ColorDrawable(
                Color.TRANSPARENT
            )
        )

        val cancelButton =
            dialog.findViewById<MaterialButton>(
                R.id.cancelButton
            )

        val confirmLogoutButton =
            dialog.findViewById<MaterialButton>(
                R.id.confirmLogoutButton
            )

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        confirmLogoutButton.setOnClickListener {

            FirebaseAuth
                .getInstance()
                .signOut()

            val intent =
                Intent(
                    this,
                    WelcomeActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            finish()
        }

        dialog.setOnShowListener {

            val window =
                dialog.window
                    ?: return@setOnShowListener

            window.setLayout(
                (
                        resources.displayMetrics.widthPixels *
                                0.88
                        ).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            window.setDimAmount(
                0.75f
            )
        }

        dialog.show()
    }

    // =============================================================
    // MAIN BUTTONS
    // =============================================================

    private fun setupClickListeners() {

        // EXPERIENCE
        binding.experienceButton.setOnClickListener {

            showExperienceDialog()
        }

        // DAILY TIME
        binding.dailyTimeButton.setOnClickListener {

            showDailyTimeDialog()
        }

        // DURATION
        binding.durationButton.setOnClickListener {

            showDurationDialog()
        }

        // START JOURNEY
        binding.generateButton.setOnClickListener {

            generateWorld()
        }


        binding.seekerQuestCard.setOnClickListener {

            dismissSeekerAttentionBadge()

            startActivityWithAnimation(
                Intent(
                    this,
                    SeekerQuestActivity::class.java
                )
            )
        }

        binding.cosmeticsCard.setOnClickListener {

            startActivityWithAnimation(
                Intent(
                    this,
                    CosmeticsActivity::class.java
                )
            )
        }
    }

    // =============================================================
    // EXPERIENCE DIALOG
    // =============================================================

    private fun showExperienceDialog() {

        showGameSelectionDialog(

            title = "CHOOSE EXPERIENCE",

            subtitle =
                "Select your adventure level",

            options = listOf(

                "Beginner" to
                        "Start your journey",

                "Intermediate" to
                        "Challenge yourself",

                "Advanced" to
                        "Push your limits"
            ),

            selected =
                selectedLevel

        ) { value ->

            selectedLevel =
                value

            binding.experienceButton.text =
                "⚔   ${value.uppercase()}"
        }
    }

    // =============================================================
    // DAILY TIME DIALOG
    // =============================================================

    private fun showDailyTimeDialog() {

        showGameSelectionDialog(

            title = "DAILY TIME",

            subtitle =
                "How much time can you play?",

            options = listOf(

                "15 min / day" to
                        "Quick daily session",

                "30 min / day" to
                        "Balanced progress",

                "45 min / day" to
                        "Strong progress",

                "60 min / day" to
                        "Deep training"
            ),

            selected =
                selectedDailyTime

        ) { value ->

            selectedDailyTime =
                value

            binding.dailyTimeButton.text =
                "◷   ${value.uppercase()}"
        }
    }

    // =============================================================
    // DURATION DIALOG
    // =============================================================

    private fun showDurationDialog() {

        showGameSelectionDialog(

            title =
                "JOURNEY DURATION",

            subtitle =
                "How long will your quest last?",

            options = listOf(

                "7 days" to
                        "A quick quest",

                "14 days" to
                        "Build momentum",

                "30 days" to
                        "Long-term adventure",

                "60 days" to
                        "Master your goal"
            ),

            selected =
                selectedDuration

        ) { value ->

            selectedDuration =
                value

            binding.durationButton.text =
                "◈   ${value.uppercase()}"
        }
    }

    // =============================================================
    // GAME SELECTION DIALOG
    // =============================================================

    private fun showGameSelectionDialog(
        title: String,
        subtitle: String,
        options: List<Pair<String, String>>,
        selected: String,
        onSelected: (String) -> Unit
    ) {

        val dialog =
            Dialog(this)

        dialog.requestWindowFeature(
            Window.FEATURE_NO_TITLE
        )

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(22),
                    dp(22),
                    dp(22),
                    dp(18)
                )

                background =
                    GradientDrawable().apply {

                        cornerRadius =
                            dp(24).toFloat()

                        setColor(
                            Color.rgb(
                                10,
                                16,
                                32
                            )
                        )

                        setStroke(
                            dp(1),
                            Color.rgb(
                                110,
                                80,
                                220
                            )
                        )
                    }
            }

        // =========================================================
        // TITLE
        // =========================================================

        val titleView =
            TextView(this).apply {

                text = title

                setTextColor(
                    Color.WHITE
                )

                textSize = 20f

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                letterSpacing =
                    0.08f

                gravity =
                    Gravity.CENTER

                alpha = 0f
            }

        container.addView(
            titleView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // =========================================================
        // SUBTITLE
        // =========================================================

        val subtitleView =
            TextView(this).apply {

                text = subtitle

                setTextColor(
                    Color.rgb(
                        160,
                        165,
                        185
                    )
                )

                textSize = 12f

                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    dp(6),
                    0,
                    dp(18)
                )

                alpha = 0f
            }

        container.addView(
            subtitleView
        )

        // =========================================================
        // OPTIONS
        // =========================================================

        options.forEach { option ->

            val value =
                option.first

            val description =
                option.second

            val card =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.HORIZONTAL

                    gravity =
                        Gravity.CENTER_VERTICAL

                    setPadding(
                        dp(16),
                        dp(12),
                        dp(12),
                        dp(12)
                    )

                    isClickable = true
                    isFocusable = true

                    background =
                        createOptionBackground(
                            value == selected
                        )
                }

            // -----------------------------------------------------
            // TEXT
            // -----------------------------------------------------

            val textContainer =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                }

            val name =
                TextView(this).apply {

                    text =
                        value.uppercase()

                    setTextColor(
                        Color.WHITE
                    )

                    textSize = 14f

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    letterSpacing =
                        0.04f
                }

            val desc =
                TextView(this).apply {

                    text = description

                    setTextColor(
                        Color.rgb(
                            145,
                            150,
                            175
                        )
                    )

                    textSize = 10f

                    setPadding(
                        0,
                        dp(3),
                        0,
                        0
                    )
                }

            textContainer.addView(
                name
            )

            textContainer.addView(
                desc
            )

            card.addView(
                textContainer
            )

            // -----------------------------------------------------
            // CHECK
            // -----------------------------------------------------

            val check =
                TextView(this).apply {

                    text =
                        if (value == selected) {
                            "✓"
                        } else {
                            ""
                        }

                    setTextColor(
                        Color.rgb(
                            190,
                            120,
                            255
                        )
                    )

                    textSize = 22f

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    gravity =
                        Gravity.CENTER

                    minWidth =
                        dp(35)
                }

            card.addView(
                check
            )

            // -----------------------------------------------------
            // CARD SIZE
            // -----------------------------------------------------

            val params =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(68)
                )

            params.bottomMargin =
                dp(10)

            container.addView(
                card,
                params
            )

            // -----------------------------------------------------
            // CLICK
            // -----------------------------------------------------

            card.setOnClickListener {

                card.animate()
                    .scaleX(0.97f)
                    .scaleY(0.97f)
                    .setDuration(70)
                    .withEndAction {

                        card.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()

                        onSelected(
                            value
                        )

                        dialog.dismiss()
                    }
                    .start()
            }
        }

        // =========================================================
        // CANCEL
        // =========================================================

        val cancel =
            TextView(this).apply {

                text = "CANCEL"

                setTextColor(
                    Color.rgb(
                        130,
                        135,
                        160
                    )
                )

                textSize = 11f

                letterSpacing =
                    0.15f

                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    dp(10),
                    0,
                    dp(4)
                )

                setOnClickListener {

                    dialog.dismiss()
                }
            }

        container.addView(
            cancel,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(40)
            )
        )

        // =========================================================
        // DIALOG
        // =========================================================

        dialog.setContentView(
            container
        )

        dialog.setCanceledOnTouchOutside(
            true
        )

        dialog.setOnShowListener {

            val window =
                dialog.window
                    ?: return@setOnShowListener

            window.setBackgroundDrawableResource(
                android.R.color.transparent
            )

            window.setLayout(
                dp(340),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            window.setGravity(
                Gravity.CENTER
            )

            titleView.animate()
                .alpha(1f)
                .setDuration(250)
                .start()

            subtitleView.animate()
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(50)
                .start()
        }

        dialog.show()

        dialog.window?.let { window ->

            window.setBackgroundDrawableResource(
                android.R.color.transparent
            )

            window.setLayout(
                dp(340),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            window.attributes =
                window.attributes.apply {

                    dimAmount =
                        0.75f
                }

            window.addFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND
            )

            window.setGravity(
                Gravity.CENTER
            )
        }
    }

    // =============================================================
    // OPTION BACKGROUND
    // =============================================================

    private fun createOptionBackground(
        selected: Boolean
    ): GradientDrawable {

        return GradientDrawable().apply {

            cornerRadius =
                dp(16).toFloat()

            if (selected) {

                setColor(
                    Color.rgb(
                        30,
                        22,
                        55
                    )
                )

                setStroke(
                    dp(2),
                    Color.rgb(
                        155,
                        95,
                        255
                    )
                )

            } else {

                setColor(
                    Color.rgb(
                        18,
                        24,
                        42
                    )
                )

                setStroke(
                    dp(1),
                    Color.rgb(
                        48,
                        55,
                        80
                    )
                )
            }
        }
    }

    // =============================================================
    // GENERATE WORLD
    // =============================================================

    private fun generateWorld() {

        val goal =
            binding.goalEditText.text
                ?.toString()
                ?.trim()
                .orEmpty()

        if (goal.isEmpty()) {

            binding.goalInputLayout.error =
                "Please enter your goal"

            binding.goalEditText.requestFocus()

            return
        }

        if (!NetworkMonitor.isOnline(this)) {
            binding.goalInputLayout.error = "Connect to the internet to create a journey"
            binding.goalEditText.requestFocus()
            return
        }

        binding.goalInputLayout.error =
            null

        val intent =
            Intent(
                this,
                GeneratingActivity::class.java
            )

        intent.putExtra(
            "GOAL",
            goal
        )

        intent.putExtra(
            "LEVEL",
            selectedLevel
        )

        intent.putExtra(
            "DAILY_TIME",
            selectedDailyTime
        )

        intent.putExtra(
            "DURATION",
            selectedDuration
        )

        startActivityWithAnimation(
            intent
        )
    }

    // =============================================================
    // DP
    // =============================================================

    private fun dp(value: Int): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }



}
