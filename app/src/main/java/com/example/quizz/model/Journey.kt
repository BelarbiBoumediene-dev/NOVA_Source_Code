package com.example.quizz.data.model

data class Journey(
    val title: String,
    val description: String,
    val goal: String,
    val level: String,
    val dailyTime: String,
    val duration: String,
    val chapters: List<Chapter>
)