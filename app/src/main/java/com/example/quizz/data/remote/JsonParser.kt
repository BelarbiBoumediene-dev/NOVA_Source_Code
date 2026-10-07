package com.example.quizz.data.remote

import com.google.gson.Gson

object JsonParser {

    private val gson = Gson()

    fun parseJourney(json: String): JourneyResponse {
        return gson.fromJson(
            json,
            JourneyResponse::class.java
        )
    }
}