package com.example.quizz.data.remote

import com.example.quizz.data.model.Challenge
import com.example.quizz.data.model.Chapter
import com.example.quizz.data.model.Journey
import com.example.quizz.data.model.Lesson
import com.example.quizz.data.model.Quiz
import com.example.quizz.data.model.QuizQuestion

object JourneyMapper {

    fun map(response: JourneyResponse): Journey {

        val chapters = response.chapters.mapIndexed { index, chapterDto ->

            val lessons = chapterDto.lessons.map { lessonDto ->

                val quizQuestions = lessonDto.quiz.map { questionDto ->
                    QuizQuestion(
                        id = questionDto.id,
                        question = questionDto.question,
                        options = questionDto.options,
                        correctAnswer = questionDto.correctAnswer,
                        explanation = questionDto.explanation
                    )
                }

                Lesson(
                    id = lessonDto.id,
                    title = lessonDto.title,
                    content = lessonDto.content,
                    example = lessonDto.example,
                    practice = lessonDto.practice,
                    estimatedMinutes = lessonDto.estimatedMinutes,
                    quiz = Quiz(
                        id = "${lessonDto.id}_quiz",
                        questions = quizQuestions,
                        passingScore = 70
                    )
                )
            }

            val challenge = chapterDto.challenge?.let { challengeDto ->

                val questions = challengeDto.questions.map { questionDto ->
                    QuizQuestion(
                        id = questionDto.id,
                        question = questionDto.question,
                        options = questionDto.options,
                        correctAnswer = questionDto.correctAnswer,
                        explanation = questionDto.explanation
                    )
                }

                Challenge(
                    id = challengeDto.id,
                    title = challengeDto.title,
                    description = challengeDto.description,
                    type = challengeDto.type,
                    passingScore = challengeDto.passingScore,
                    questions = questions
                )
            }

            Chapter(
                id = chapterDto.id,
                title = chapterDto.title,
                description = chapterDto.description,
                order = index + 1,
                lessons = lessons,
                challenge = challenge,
                isLocked = index != 0
            )
        }

        return Journey(
            title = response.title,
            description = response.description,
            goal = response.goal,
            level = response.level,
            dailyTime = response.dailyTime,
            duration = response.duration,
            chapters = chapters
        )
    }
}

fun JourneyResponse.toJourney(): Journey {
    return JourneyMapper.map(this)
}