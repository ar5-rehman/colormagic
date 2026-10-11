package com.colormagic.kids.domain.repository

/** The kind of message a parent is sending from the Support screen. */
enum class FeedbackType(val wireValue: String) {
    Suggestion("suggestion"),
    Bug("bug"),
    Question("question")
}

/** Sends parent feedback to the backend (stored for the developer to review). */
interface FeedbackRepository {
    /**
     * @param type    suggestion / bug / question
     * @param message the parent's message (required, non-blank)
     * @param email   optional reply-to address
     */
    suspend fun submit(
        type: FeedbackType,
        message: String,
        email: String?
    ): Result<Unit>

    /**
     * Flags an AI-generated picture as inappropriate (Google Play's AI-content
     * policy requires in-app reporting). Sent as a "report" feedback entry with
     * the sketch's id, prompt and image link so it can be reviewed.
     */
    suspend fun reportImage(
        sketchId: String,
        prompt: String,
        imageUrl: String?
    ): Result<Unit>
}
