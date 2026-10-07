package com.example.quizz.data.model

data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val passingScore: Int = 70,
    val questions: List<QuizQuestion>,
    val completed: Boolean = false
)