# 🚀 KNotes: Premium Productivity Ecosystem

<div align="center">

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?style=flat\&logo=kotlin)](https://kotlinlang.org)
[![Material 3](https://img.shields.io/badge/Design-Material--3-blue.svg?style=flat)](https://m3.material.io)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-green.svg?style=flat)]()
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-orange.svg?style=flat\&logo=android)](https://www.android.com)

### ✨ AI-Powered Note Taking • Material 3 Design • Modern Productivity

KNotes is a premium Android productivity suite built with **Kotlin**, **Material 3**, and **Clean Architecture**. Designed to help users capture ideas, manage tasks, organize knowledge, and streamline workflows with a beautiful and responsive user experience.

</div>

---

## 📐 Architecture: Scalable & Clean

KNotes is built upon a formal **Clean Architecture** foundation, ensuring high testability, maintainability, and separation of concerns.

*   **Domain Layer**: Pure Kotlin business logic containing Models, Repository Interfaces, and UseCases.
*   **Data Layer**: Room local storage, Firebase remote storage, and Repository implementations with Mappers.
*   **Presentation Layer**: MVVM pattern using StateFlow and a hybrid of XML Fragments and Jetpack Compose.
*   **DI Layer**: Robust dependency injection powered by Hilt.

---

## 🌟 Key Features

### 📝 Smart Note Management
*   **Rich Editor**: HTML-based rich text with formatting, undo/redo, and background colors.
*   **Organization**: Categorize notes into **Folders** and manage them with a many-to-many **Tagging** system.
*   **Organization View**: Toggle between **Grid and List** layouts.

### ✅ Task & Productivity
*   **Unified Tasks**: Manage pending and completed tasks with deadlines and priority levels.
*   **Reminders**: High-precision notifications scheduled via AlarmManager with boot recovery.
*   **Note-Task Linking**: Associate tasks directly with relevant notes for better context.

### 🔍 Unified Intelligence
*   **Global Search**: Instant, debounced search across all notes and tasks with search history.
*   **AI Assistant**: Powered by **Google Gemini** for summarization, task extraction, grammar polishing, and Q&A.
*   **Privacy-First AI**: Explicit user consent and local toggles for AI data processing.

### ☁️ Cloud & Backup
*   **Firebase Sync**: Secure cross-device synchronization via Firestore and Firebase Auth.
*   **Data Sovereignty**: Export your entire database to **JSON** or individual notes to **Text (.txt)**.
*   **Offline First**: Fully functional without internet, with seamless background synchronization.

---

## 🛠 Tech Stack

| Layer                | Technology                        |
| -------------------- | --------------------------------- |
| Language             | Kotlin (Coroutines + Flow)        |
| UI Framework         | Material 3 + Jetpack Compose      |
| Architecture         | Clean Architecture + MVVM          |
| Dependency Injection | Hilt                              |
| Database             | Room                              |
| Cloud Services       | Firebase Auth + Firestore         |
| AI Engine            | Google Gemini SDK                 |
| Background Tasks     | WorkManager                       |
| Preferences          | DataStore                         |

---

## 🚀 Installation & Setup

### 1️⃣ API Key Configuration
KNotes AI features require a Gemini API key. 
1. Get a key from [Google AI Studio](https://aistudio.google.com/).
2. Add the following to your `local.properties`:
   ```properties
   AI_API_KEY=your_actual_key_here
   ```

### 2️⃣ Firebase Setup
1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
2. Add your `google-services.json` to the `app/` directory.
3. Enable Email/Password Auth and Cloud Firestore.

---

## 🤝 Portfolio Value
This project demonstrates proficiency in:
*   **Modern Android Development**: Compose, Hilt, Room, WorkManager.
*   **Enterprise Architecture**: Multi-layered Clean Architecture.
*   **AI Integration**: Practical implementation of LLMs in mobile apps.
*   **Security**: Biometric security, encrypted preferences, and secure API handling.

---

<div align="center">

### ⭐ Professional • Reliable • Intelligent

Made with ❤️ by **Akash**

</div>
