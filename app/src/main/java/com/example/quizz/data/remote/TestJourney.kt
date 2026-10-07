package com.example.quizz.data.remote

object TestJourney {

    val json = """
        {
          "title": "Kotlin Adventure",
          "description": "Learn Kotlin and start building Android apps.",
          "goal": "Learn Kotlin and build my first Android app",
          "level": "Beginner",
          "dailyTime": "30 min / day",
          "duration": "30 days",
          "chapters": [
            {
              "id": "chapter_1",
              "title": "The Village of Kotlin",
              "description": "Learn the foundations of Kotlin.",
              "order": 1,
              "lessons": [
                {
                  "id": "lesson_1",
                  "title": "Your First Kotlin Code",
                  "content": "Learn variables and basic Kotlin syntax.",
                  "estimatedMinutes": 15
                }
              ],
              "quiz": {
                "id": "quiz_1",
                "passingScore": 70,
             "questions": [
  {
    "id": "question_1",
    "question": "Which keyword creates a mutable variable in Kotlin?",
    "options": [
      "val",
      "var",
      "fun",
      "class"
    ],
    "correctAnswer": 1,
    "explanation": "The var keyword creates a mutable variable, which means its value can be changed."
  },
  {
    "id": "question_2",
    "question": "Which keyword is used to create a function in Kotlin?",
    "options": [
      "var",
      "fun",
      "val",
      "object"
    ],
    "correctAnswer": 1,
    "explanation": "The fun keyword is used to declare a function in Kotlin."
  },
  {
    "id": "question_3",
    "question": "Which Kotlin keyword declares a read-only variable?",
    "options": [
      "var",
      "fun",
      "val",
      "data"
    ],
    "correctAnswer": 2,
    "explanation": "The val keyword creates a read-only reference. Its value cannot be reassigned after initialization."
  },
  {
    "id": "question_4",
    "question": "Which keyword is used to define a class in Kotlin?",
    "options": [
      "class",
      "fun",
      "val",
      "when"
    ],
    "correctAnswer": 0,
    "explanation": "The class keyword is used to declare a class in Kotlin."
  }
]
              },
              "challenge": {
                "id": "challenge_1",
                "title": "Create Your First Variable",
                "description": "Create a Kotlin variable containing your name.",
                "type": "coding"
              }
            },
              {
              "id": "chapter_2",
              "title": "algebre",
              "description": "Learn the foundations of Kotlin.",
              "order": 2,
              "lessons": [
                {
                  "id": "lesson_2",
                  "title": "Your First Kotlin Code",
                  "content": "Learn variables and basic Kotlin syntax.",
                  "estimatedMinutes": 15
                }
              ],
              "quiz": {
                "id": "quiz_1",
                "passingScore": 70,
                "questions": [
                  {
                    "id": "question_2",
                    "question": "Which keyword creates a mutable variable in Kotlin?",
                    "options": [
                      "val",
                      "var",
                      "fun",
                      "class"
                    ],
                    "correctAnswer": 2,
                    "explanation": "The var keyword creates a mutable variable."
                  }
                ]
              },
              "challenge": {
                "id": "challenge_2",
                "title": "Create Your First Variable",
                "description": "Create a Kotlin variable containing your name.",
                "type": "coding"
              }
            }
          ]
        }
    """.trimIndent()
}