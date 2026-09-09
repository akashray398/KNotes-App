package com.example.knotes.util

object AiPromptTemplates {
    const val SUMMARIZE = "Summarize the following note content concisely. Focus on key points and takeaways:\n\n"
    const val IMPROVE_GRAMMAR = "Improve the grammar, clarity, and flow of the following text while strictly preserving its original meaning and tone:\n\n"
    const val REWRITE_PREFIX = "Rewrite the following text in a "
    const val REWRITE_SUFFIX = " style. Ensure the core message remains unchanged:\n\n"
    const val GENERATE_TAGS = "Analyze the following note content and provide a comma-separated list of 3 to 5 relevant, one-word tags (no hashtags):\n\n"
    const val EXTRACT_TASKS = "Identify all actionable items and tasks from the following text. Return them as a simple bulleted list starting with '-':\n\n"
    const val GENERATE_TITLE = "Based on the content provided, suggest a single, concise, and descriptive title (maximum 6 words):\n\n"
    const val ASK_QUESTION_PREFIX = "Context: The following is a personal note.\n\n"
    const val ASK_QUESTION_MID = "\n\nQuestion: "
    const val ASK_QUESTION_SUFFIX = "\n\nPlease provide a direct answer based only on the note content."
}
