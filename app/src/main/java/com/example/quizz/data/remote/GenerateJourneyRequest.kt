package com.example.quizz.data.remote

data class GenerateJourneyRequest(
    val goal: String,
    val level: String,
    val dailyTime: String,
    val duration: String
)