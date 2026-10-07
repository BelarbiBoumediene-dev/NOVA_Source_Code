package com.example.quizz.data.model

data class Chapter(
    val id: String,
    val title: String,
    val description: String,
    val order: Int,
    val lessons: List<Lesson>,
    val challenge: Challenge?,
    val isLocked: Boolean = true
)