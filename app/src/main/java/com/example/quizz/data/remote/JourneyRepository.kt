package com.example.quizz.data.remote

import java.io.IOException

class JourneyRequestException(val statusCode: Int) : IOException()

class JourneyRepository {

    suspend fun generateJourney(
        goal: String,
        level: String,
        dailyTime: String,
        duration: String
    ): JourneyResponse {

        val response = RetrofitInstance.api.generateJourney(
            GenerateJourneyRequest(
                goal = goal,
                level = level,
                dailyTime = dailyTime,
                duration = duration
            )
        )

        if (!response.isSuccessful) {
            throw JourneyRequestException(response.code())
        }

        return response.body()
            ?: throw Exception("Empty response from server")
    }
}
