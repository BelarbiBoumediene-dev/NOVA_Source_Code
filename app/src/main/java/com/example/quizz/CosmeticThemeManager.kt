package com.example.quizz

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.button.MaterialButton

object CosmeticThemeManager {

    private const val PREFS_NAME = "ai_quest_cosmetics"
    private const val KEY_SELECTED_COSMETIC = "selected_cosmetic"

    data class Palette(
        val background: Int,
        val surface: Int,
        val primary: Int,
        val secondary: Int,
        val muted: Int,
        val outline: Int
    )

    fun selectedCosmetic(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED_COSMETIC, FirebaseProgressManager.DEFAULT_COSMETIC)
            ?: FirebaseProgressManager.DEFAULT_COSMETIC
    }

    fun cacheSelectedCosmetic(context: Context, cosmeticId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_COSMETIC, cosmeticId)
            .apply()
    }

    fun palette(context: Context): Palette {
        return paletteFor(selectedCosmetic(context))
    }

    fun paletteFor(cosmeticId: String): Palette {
        return when (cosmeticId) {
            "nova_neon" -> Palette(
                background = Color.rgb(6, 10, 28),
                surface = Color.rgb(13, 21, 48),
                primary = Color.rgb(0, 216, 255),
                secondary = Color.rgb(255, 79, 216),
                muted = Color.rgb(139, 169, 205),
                outline = Color.rgb(40, 102, 160)
            )

            "nova_solar" -> Palette(
                background = Color.rgb(12, 12, 16),
                surface = Color.rgb(20, 20, 24),
                primary = Color.rgb(255, 176, 0),
                secondary = Color.rgb(255, 217, 102),
                muted = Color.rgb(175, 165, 140),
                outline = Color.rgb(82, 65, 35)
            )

            else -> Palette(
                background = Color.rgb(8, 12, 23),
                surface = Color.rgb(16, 22, 41),
                primary = Color.rgb(79, 140, 255),
                secondary = Color.rgb(139, 92, 246),
                muted = Color.rgb(120, 131, 159),
                outline = Color.rgb(40, 52, 82)
            )
        }
    }

    fun apply(activity: Activity) {
        val palette = palette(activity)
        activity.window.statusBarColor = palette.background
        activity.window.navigationBarColor = palette.background
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        content.getChildAt(0)?.let { applyRootBackground(it, palette) }
        applyToView(content, palette)
    }

    fun avatarForLevel(level: Int, cosmeticId: String): Int {
        val tier = when {
            level >= 100 -> 8
            level >= 75 -> 7
            level >= 50 -> 6
            level >= 35 -> 5
            level >= 20 -> 4
            level >= 10 -> 3
            level >= 5 -> 2
            else -> 1
        }

        return when (cosmeticId) {
            "nova_neon" -> neonAvatar(tier)
            "nova_solar" -> solarAvatar(tier)
            else -> classicAvatar(tier)
        }
    }

    fun resultImageForScore(score: Int, cosmeticId: String): Int {
        val tier = when {
            score > 74 -> 3
            score > 49 -> 1
            else -> 2
        }

        return when (cosmeticId) {
            "nova_neon" -> when (tier) {
                3 -> R.drawable.imgn3
                2 -> R.drawable.imgn2
                else -> R.drawable.imgn1
            }

            "nova_solar" -> when (tier) {
                3 -> R.drawable.imgs3
                2 -> R.drawable.imgs2
                else -> R.drawable.imgs1
            }

            else -> when (tier) {
                3 -> R.drawable.img3
                2 -> R.drawable.img2
                else -> R.drawable.img1
            }
        }
    }

    fun stylePrimaryAction(button: MaterialButton) {
        val palette = palette(button.context)
        button.backgroundTintList = primaryButtonTint(palette.primary)
        button.rippleColor = ColorStateList.valueOf(
            Color.argb(46, 255, 255, 255)
        )
    }

    fun primaryButtonTint(color: Int): ColorStateList {
        return ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_pressed),
                intArrayOf()
            ),
            intArrayOf(
                darken(color, 0.84f),
                color
            )
        )
    }

    private fun classicAvatar(tier: Int): Int = when (tier) {
        8 -> R.drawable.m8
        7 -> R.drawable.m7
        6 -> R.drawable.m6
        5 -> R.drawable.m5
        4 -> R.drawable.m4
        3 -> R.drawable.m3
        2 -> R.drawable.m2
        else -> R.drawable.m1
    }

    private fun neonAvatar(tier: Int): Int = when (tier) {
        8 -> R.drawable.n8
        7 -> R.drawable.n7
        6 -> R.drawable.n6
        5 -> R.drawable.n5
        4 -> R.drawable.n4
        3 -> R.drawable.n3
        2 -> R.drawable.n2
        else -> R.drawable.n1
    }

    private fun solarAvatar(tier: Int): Int = when (tier) {
        8 -> R.drawable.s8
        7 -> R.drawable.s7
        6 -> R.drawable.s6
        5 -> R.drawable.s5
        4 -> R.drawable.s4
        3 -> R.drawable.s3
        2 -> R.drawable.s2
        else -> R.drawable.s1
    }

    private fun applyRootBackground(view: View, palette: Palette) {
        val background = view.background as? ColorDrawable ?: return
        if (background.color in defaultBackgroundColors) {
            view.setBackgroundColor(palette.background)
        }
    }

    private fun applyToView(view: View, palette: Palette) {
        when (view) {
            is MaterialButton -> {
                remapTextColor(view, palette)
                if (view.id in primaryActionButtonIds) {
                    stylePrimaryAction(view)
                }
            }

            is TextView -> remapTextColor(view, palette)
            is ProgressBar -> view.progressTintList = ColorStateList.valueOf(palette.primary)
        }

        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                applyToView(view.getChildAt(index), palette)
            }
        }
    }

    private fun remapTextColor(view: TextView, palette: Palette) {
        view.setTextColor(
            when (view.currentTextColor) {
                in primaryAccentColors -> palette.primary
                in secondaryAccentColors -> palette.secondary
                else -> view.currentTextColor
            }
        )
    }

    private val primaryActionButtonIds = setOf(
        R.id.generateButton,
        R.id.createJourneyButton,
        R.id.practiceButton,
        R.id.checkButton,
        R.id.completeButton,
        R.id.doneButton,
        R.id.seekerQuestAction
    )

    private val primaryAccentColors = setOf(
        Color.rgb(79, 140, 255),
        Color.rgb(110, 168, 255),
        Color.rgb(0, 216, 255),
        Color.rgb(255, 176, 0)
    )

    private val secondaryAccentColors = setOf(
        Color.rgb(139, 92, 246),
        Color.rgb(167, 139, 250),
        Color.rgb(255, 79, 216),
        Color.rgb(255, 217, 102)
    )

    private val defaultBackgroundColors = setOf(
        Color.rgb(8, 12, 23),
        Color.rgb(12, 17, 32)
    )

    private fun darken(color: Int, factor: Float): Int {
        return Color.rgb(
            (Color.red(color) * factor).toInt(),
            (Color.green(color) * factor).toInt(),
            (Color.blue(color) * factor).toInt()
        )
    }
}
