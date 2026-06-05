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
}
