package com.example.quizz

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

data class SavedJourney(
    val id: String,
    val title: String,
    val goal: String,
    val level: String,
    val dailyTime: String,
    val duration: String,
    val journeyJson: String
)

object JourneyStorage {

    private const val PREFS_NAME = "ai_quest_journeys"
    private const val KEY_JOURNEYS = "saved_journeys"

    private val gson = Gson()

    private fun prefs(context: Context) =
        context.getSharedPreferences(
            "${PREFS_NAME}_${currentUserScope()}",
            Context.MODE_PRIVATE
        )

    private fun currentUserScope(): String {
        return FirebaseAuth.getInstance().currentUser?.uid
            ?: "signed_out"
    }

    private fun getType() =
        object : TypeToken<MutableList<SavedJourney>>() {}.type

    fun getJourneys(
        context: Context
    ): MutableList<SavedJourney> {

        val json =
            prefs(context)
                .getString(KEY_JOURNEYS, null)

        if (json.isNullOrEmpty()) {
            return mutableListOf()
        }

        return try {

            gson.fromJson(
                json,
                getType()
            ) ?: mutableListOf()

        } catch (e: Exception) {

            e.printStackTrace()

            mutableListOf()
        }
    }

    private fun saveJourneys(
        context: Context,
        journeys: List<SavedJourney>
    ) {

        val json =
            gson.toJson(journeys)

        prefs(context)
            .edit()
            .putString(
                KEY_JOURNEYS,
                json
            )
            .apply()
    }

    fun addJourney(
        context: Context,
        journeyJson: String,
        title: String,
        goal: String,
        level: String,
        dailyTime: String,
        duration: String
    ): SavedJourney {

        val journeys =
            getJourneys(context)

        val journey =
            SavedJourney(
                id = UUID.randomUUID().toString(),
                title = title,
                goal = goal,
                level = level,
                dailyTime = dailyTime,
                duration = duration,
                journeyJson = journeyJson
            )

        journeys.add(journey)

        saveJourneys(
            context,
            journeys
        )

        return journey
    }

    fun getJourney(
        context: Context,
        journeyId: String
    ): SavedJourney? {

        return getJourneys(context)
            .find {
                it.id == journeyId
            }
    }

    fun deleteJourney(
        context: Context,
        journeyId: String
    ) {

        val journeys =
            getJourneys(context)

        journeys.removeAll {
            it.id == journeyId
        }

        saveJourneys(
            context,
            journeys
        )
    }

    fun updateJourney(
        context: Context,
        journey: SavedJourney
    ) {

        val journeys =
            getJourneys(context)

        val index =
            journeys.indexOfFirst {
                it.id == journey.id
            }

        if (index != -1) {

            journeys[index] =
                journey

            saveJourneys(
                context,
                journeys
            )
        }
    }

    fun hasJourneys(
        context: Context
    ): Boolean {

        return getJourneys(context)
            .isNotEmpty()
    }
}
