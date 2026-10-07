# 🌌 NOVA — AI Quest Companion

<p align="center">
  <img src="https://img.shields.io/badge/Android-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/Android-SDK-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Solana-Mobile-9945FF?style=for-the-badge&logo=solana&logoColor=white"/>
  <img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black"/>
</p>

<p align="center">
  <b>Turn your learning goals into interactive quests.</b>
</p>

---

## 🚀 About NOVA

**NOVA** is an AI-powered Android learning companion designed to transform a learning goal into a personalized and gamified learning journey.

Instead of presenting learning as a traditional list of courses, NOVA turns it into a **quest-based experience** with:

- 🧭 Personalized learning journeys
- 📚 Structured lessons
- 🧠 Interactive quizzes
- ⚔️ Practical challenges
- 🏆 Achievements
- 🪙 In-app Coins and rewards
- 🤖 An interactive Nova companion
- 🔗 Solana wallet-based achievement verification

---

## ✨ Key Features

### 🤖 AI Learning Journeys

NOVA can generate a structured learning journey based on the learner's:

- Learning goal
- Current level
- Daily available time
- Learning duration

The generated journey is organized into chapters, lessons, quizzes and practical challenges.

### 📚 Interactive Lessons

Each lesson can include:

- Educational content
- Practice activities
- Knowledge quizzes
- Explanations for answers

### 🎯 Quests & Challenges

Learning is presented as a progression system rather than a static course.

Users can complete quests and practical challenges while progressing through their learning journey.

### 🏆 Achievements

Users can unlock achievements by completing specific activities inside NOVA.

Achievements can also be verified on the Solana blockchain through the Solana Mobile Wallet Adapter.

### 🔗 Solana Integration

NOVA integrates the **Solana Mobile Wallet Adapter** to allow users to connect their mobile wallet and verify achievements on-chain.

**Network:** Solana Devnet

The application creates a transaction containing achievement information using the Solana Memo program.

> Note: NOVA currently uses Solana wallet/on-chain achievement verification. It does **not** currently integrate the SKR token.

---

## 🪙 Gamification

NOVA includes an internal reward system using **Coins**.

Coins can be earned through activities such as completing learning quests and quizzes.

The Coins system is an **in-app reward mechanism** and is separate from Solana's SKR token.

---

## 🧩 Technology Stack

| Technology | Usage |
|---|---|
| Kotlin | Android application development |
| XML | Android UI |
| Android SDK | Mobile platform |
| AndroidX | Android architecture and components |
| Material Components | UI components |
| Kotlin Coroutines | Asynchronous operations |
| Firebase | Application backend / data |
| Gemini API | AI-generated learning journeys |
| Solana Mobile Wallet Adapter | Wallet connection |
| Solana Web3 | Solana interaction |

---

## 🏗️ Architecture

```text
NOVA
│
├── Android Application
│   ├── Kotlin
│   ├── XML UI
│   ├── Activities
│   ├── Animations
│   └── Gamification
│
├── AI Learning System
│   ├── Learning Goal
│   ├── User Level
│   ├── Daily Time
│   └── Duration
│
├── Quest Engine
│   ├── Chapters
│   ├── Lessons
│   ├── Quizzes
│   └── Challenges
│
├── Solana Integration
│   ├── Mobile Wallet Adapter
│   ├── Wallet Authorization
│   └── Achievement Verification
│
└── Firebase
    └── User / Progress Data
