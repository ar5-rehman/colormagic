package com.colormagic.kids.domain.repository

import com.colormagic.kids.domain.model.Sketch
import com.colormagic.kids.domain.model.UserQuota

// Outcome of a sketch-generation request. A sealed type (rather than a bare
// Result) so the ViewModel can `when` over every meaningful case — each maps
// to a distinct UI state — without inspecting exceptions.
sealed interface SketchGenerationResult {
    /** Backend generated the sketch and a credit was spent. */
    data class Success(val sketch: Sketch) : SketchGenerationResult

    /** Account is out of credits — show the "ask a grown-up" message. */
    data object NoCredits : SketchGenerationResult

    /** Prompt was rejected (unsafe / empty / too long). [message] is
     *  backend-supplied and already kid-safe to display. */
    data class Rejected(val message: String) : SketchGenerationResult

    /** Network/server failure — transient, the user can retry. */
    data class Failed(val message: String) : SketchGenerationResult
}

// The single contract for talking to the Firebase backend's sketch features.
// Implementations call the Cloud Functions callables — the OpenAI key stays
// server-side, and Auth + App Check tokens ride along automatically.
interface SketchRepository {

    /** Calls `generateSketch`. Never throws — every outcome is a result case. */
    suspend fun generateSketch(prompt: String): SketchGenerationResult

    /** Calls `userQuota`. Returns the credit snapshot, or failure on network error. */
    suspend fun getQuota(): Result<UserQuota>
}
