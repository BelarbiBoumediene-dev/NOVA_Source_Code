package com.example.quizz.data.model

data class Lesson(
    val id: String,
    val title: String,
    val content: String,
    val example: String,
    val practice: String,
    val estimatedMinutes: Int,
    val quiz: Quiz,
    val completed: Boolean = false
)