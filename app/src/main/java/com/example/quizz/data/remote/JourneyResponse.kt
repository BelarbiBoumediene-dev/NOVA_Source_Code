package com.example.quizz.data.remote

data class JourneyResponse(
    val title: String,
    val description: String,
    val goal: String,
    val level: String,
    val dailyTime: String,
    val duration: String,
    val chapters: List<ChapterDto>
)

data class ChapterDto(
    val id: String,
    val title: String,
    val description: String,
    val lessons: List<LessonDto>,
    val challenge: ChallengeDto?
)

data class LessonDto(
    val id: String,
    val title: String,
    val content: String,
    val example: String,
    val practice: String,
    val estimatedMinutes: Int,
    val quiz: List<QuizQuestionDto>
)

data class QuizQuestionDto(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String
)

data class ChallengeDto(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val passingScore: Int,
    val questions: List<QuizQuestionDto>
)