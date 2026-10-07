package com.example.quizz

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class CosmeticsActivity : AppCompatActivity() {

    private lateinit var coinsText: TextView

    private lateinit var defaultButton: MaterialButton
    private lateinit var neonButton: MaterialButton
    private lateinit var solarButton: MaterialButton

    private lateinit var defaultCard: MaterialCardView
    private lateinit var neonCard: MaterialCardView
    private lateinit var solarCard: MaterialCardView

    private lateinit var defaultStatus: TextView
    private lateinit var neonStatus: TextView
    private lateinit var solarStatus: TextView

    private val ownedCosmetics = mutableSetOf<String>()

    // Prevent multiple purchases at the same time
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_cosmetics)

        CosmeticThemeManager.apply(this)

        coinsText = findViewById(R.id.coinsText)

        defaultButton = findViewById(R.id.defaultSkinButton)
        neonButton = findViewById(R.id.neonSkinButton)
        solarButton = findViewById(R.id.solarSkinButton)

        defaultCard = findViewById(R.id.defaultCard)
        neonCard = findViewById(R.id.neonCard)
        solarCard = findViewById(R.id.solarCard)

        defaultStatus = findViewById(R.id.defaultStatus)
        neonStatus = findViewById(R.id.neonStatus)
        solarStatus = findViewById(R.id.solarStatus)

        findViewById<TextView>(R.id.backButton).setOnClickListener {
            finish()
        }

        loadAvatars()
        loadOwnedCosmetics()

        defaultButton.setOnClickListener {
            selectCosmetic("nova_default", 0)
        }

        neonButton.setOnClickListener {
            selectCosmetic("nova_neon", 250)
        }

        solarButton.setOnClickListener {
            selectCosmetic("nova_solar", 700)
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadAvatars() {

        val defaultAvatar = findViewById<ImageView>(R.id.defaultAvatar)
        val neonAvatar = findViewById<ImageView>(R.id.neonAvatar)
        val solarAvatar = findViewById<ImageView>(R.id.solarAvatar)

        Glide.with(this)
            .asGif()
            .load(R.drawable.msgif)
            .circleCrop()
            .into(defaultAvatar)

        Glide.with(this)
            .asGif()
            .load(R.drawable.msgif2)
            .circleCrop()
            .into(neonAvatar)

        Glide.with(this)
            .asGif()
            .load(R.drawable.msgif3)
            .circleCrop()
            .into(solarAvatar)
    }

    private fun loadOwnedCosmetics() {

        ownedCosmetics.clear()

        val preferences = getSharedPreferences(
            "cosmetics",
            MODE_PRIVATE
        )

        // Classic is always free.
        ownedCosmetics.add("nova_default")

        if (preferences.getBoolean("nova_neon_owned", false)) {
            ownedCosmetics.add("nova_neon")
        }

        if (preferences.getBoolean("nova_solar_owned", false)) {
            ownedCosmetics.add("nova_solar")
        }
    }

    private fun saveOwnedCosmetic(id: String) {

        getSharedPreferences(
            "cosmetics",
            MODE_PRIVATE
        )
            .edit()
            .putBoolean("${id}_owned", true)
            .apply()

        ownedCosmetics.add(id)
    }

    private fun loadProfile() {

        lifecycleScope.launch {

            FirebaseProgressManager
                .getCosmeticProfile()
                .onSuccess { profile ->

                    if (!isFinishing && !isDestroyed) {
                        render(profile)
                    }
                }
                .onFailure { error ->

                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(
                            this@CosmeticsActivity,
                            error.message ?: "Unable to load cosmetics",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    private fun selectCosmetic(
        id: String,
        cost: Int
    ) {

        // Ignore repeated clicks while processing.
        if (isProcessing) {
            return
        }

        isProcessing = true
        setButtonsEnabled(false)

        lifecycleScope.launch {

            FirebaseProgressManager
                .purchaseCosmetic(id, cost)
                .onSuccess { profile ->

                    // Paid cosmetics are remembered locally.
                    if (cost > 0) {
                        saveOwnedCosmetic(id)
                    }

                    CosmeticThemeManager.cacheSelectedCosmetic(
                        this@CosmeticsActivity,
                        profile.selectedCosmetic
                    )

                    CosmeticThemeManager.apply(
                        this@CosmeticsActivity
                    )

                    render(profile)

                    Toast.makeText(
                        this@CosmeticsActivity,
                        if (cost == 0) {
                            "Classic Nova equipped"
                        } else {
                            "Nova style equipped"
                        },
                        Toast.LENGTH_SHORT
                    ).show()

                    isProcessing = false
                    setButtonsEnabled(true)
                }
                .onFailure { error ->

                    isProcessing = false
                    setButtonsEnabled(true)

                    Toast.makeText(
                        this@CosmeticsActivity,
                        error.message ?: "Unable to unlock style",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {

        defaultButton.isEnabled = enabled
        neonButton.isEnabled = enabled
        solarButton.isEnabled = enabled
    }

    private fun render(
        profile: FirebaseProgressManager.CosmeticProfile
    ) {

        val selected = profile.selectedCosmetic

        CosmeticThemeManager.cacheSelectedCosmetic(
            this,
            selected
        )

        CosmeticThemeManager.apply(this)

        coinsText.text = "◈  ${profile.coins}"

        renderItem(
            button = defaultButton,
            card = defaultCard,
            status = defaultStatus,
            id = "nova_default",
            price = 0,
            selected = selected
        )

        renderItem(
            button = neonButton,
            card = neonCard,
            status = neonStatus,
            id = "nova_neon",
            price = 250,
            selected = selected
        )

        renderItem(
            button = solarButton,
            card = solarCard,
            status = solarStatus,
            id = "nova_solar",
            price = 700,
            selected = selected
        )
    }

    private fun renderItem(
        button: MaterialButton,
        card: MaterialCardView,
        status: TextView,
        id: String,
        price: Int,
        selected: String
    ) {

        val isSelected = selected == id
        val isOwned = ownedCosmetics.contains(id)

        if (isSelected) {

            // SELECTED
            card.setCardBackgroundColor(
                Color.parseColor("#171B2B")
            )

            card.strokeWidth = dp(2)

            card.strokeColor = when (id) {
                "nova_neon" ->
                    Color.parseColor("#00B8D4")

                "nova_solar" ->
                    Color.parseColor("#F2B450")

                else ->
                    Color.parseColor("#7060E8")
            }

            button.text = "✔"

            button.setTextColor(Color.WHITE)

            button.backgroundTintList =
                ColorStateList.valueOf(
                    Color.parseColor("#7060E8")
                )

            status.text = "EQUIPPED"

            status.setTextColor(
                Color.parseColor("#8E7CFF")
            )

        } else if (isOwned) {

            // OWNED BUT NOT SELECTED
            card.setCardBackgroundColor(
                Color.parseColor("#111726")
            )

            card.strokeWidth = dp(1)

            card.strokeColor =
                Color.parseColor("#252E43")

            button.text = "EQUIP"

            button.setTextColor(Color.WHITE)

            button.backgroundTintList =
                ColorStateList.valueOf(
                    Color.parseColor("#252E43")
                )

            status.text = "OWNED"

            status.setTextColor(
                Color.parseColor("#7C88A8")
            )

        } else {

            // NOT OWNED
            card.setCardBackgroundColor(
                Color.parseColor("#111726")
            )

            card.strokeWidth = dp(1)

            card.strokeColor =
                Color.parseColor("#252E43")

            button.text =
                if (price == 0) {
                    "FREE"
                } else {
                    "$price ◈"
                }

            button.setTextColor(Color.WHITE)

            button.backgroundTintList =
                ColorStateList.valueOf(
                    when (id) {
                        "nova_neon" ->
                            Color.parseColor("#006B7C")

                        "nova_solar" ->
                            Color.parseColor("#51438A")

                        else ->
                            Color.parseColor("#5146A5")
                    }
                )

            status.text =
                if (price == 0) {
                    "FREE"
                } else {
                    "$price COINS"
                }

            status.setTextColor(
                when (id) {
                    "nova_neon" ->
                        Color.parseColor("#00B8D4")

                    "nova_solar" ->
                        Color.parseColor("#A78BFA")

                    else ->
                        Color.parseColor("#7C88A8")
                }
            )
        }

        button.alpha = 1f
    }

    private fun dp(value: Int): Int {

        return (
                value * resources.displayMetrics.density
                ).toInt()
    }
}
