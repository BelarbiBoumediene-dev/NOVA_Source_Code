package com.example.quizz.data.model

data class Quiz(
    val id: String,
    val questions: List<QuizQuestion>,
    val passingScore: Int = 70,
    val score: Int? = null,
    val completed: Boolean = false
)