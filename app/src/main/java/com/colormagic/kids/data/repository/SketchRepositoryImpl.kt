package com.colormagic.kids.data.repository

import com.colormagic.kids.data.telemetry.AppTelemetry
import com.colormagic.kids.domain.model.Sketch
import com.colormagic.kids.domain.model.UserQuota
import com.colormagic.kids.domain.repository.SketchGenerationResult
import com.colormagic.kids.domain.repository.SketchRepository
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// Talks to the Firebase backend via callable functions. The Functions SDK
// attaches the user's Auth token + an App Check token on every call, so the
// backend can trust the caller and the OpenAI key never ships in the app.
@Singleton
class SketchRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions,
    private val telemetry: AppTelemetry
) : SketchRepository {

    override suspend fun generateSketch(prompt: String): SketchGenerationResult {
        return try {
            val response = functions
                .getHttpsCallable(FN_GENERATE_SKETCH)
                .call(mapOf("prompt" to prompt))
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = response.getData() as Map<String, Any?>
            val sketchId = data["sketchId"] as? String
            val imageUrl = data["imageUrl"] as? String
            if (sketchId == null || imageUrl == null) {
                return SketchGenerationResult.Failed(GENERIC_ERROR)
            }
            SketchGenerationResult.Success(
                Sketch(id = sketchId, prompt = prompt, imageUrl = imageUrl)
            )
        } catch (e: FirebaseFunctionsException) {
            // The backend throws HttpsError; the SDK maps it to a code here.
            // NoCredits and Rejected are expected business outcomes, not bugs —
            // we only record the unclassified "Failed" path to Crashlytics.
            when (e.code) {
                FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                    SketchGenerationResult.NoCredits
                FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                    // Backend message is already kid-safe ("Please try a safe…").
                    SketchGenerationResult.Rejected(e.message ?: GENERIC_ERROR)
                else -> {
                    telemetry.recordNonFatal(e)
                    SketchGenerationResult.Failed(e.message ?: GENERIC_ERROR)
                }
            }
        } catch (e: Exception) {
            telemetry.recordNonFatal(e)
            SketchGenerationResult.Failed(e.message ?: GENERIC_ERROR)
        }
    }

    override suspend fun getQuota(): Result<UserQuota> = runCatching {
        val response = functions
            .getHttpsCallable(FN_USER_QUOTA)
            .call(emptyMap<String, Any>())
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = response.getData() as Map<String, Any?>
        UserQuota(
            plan = data["plan"] as? String ?: "free",
            subscriptionActive = data["subscriptionActive"] as? Boolean ?: false,
            remainingFreeSketches = (data["remainingFreeSketches"] as? Number)?.toInt() ?: 0,
            remainingMonthlySketches = (data["remainingMonthlySketches"] as? Number)?.toInt() ?: 0,
            extraCredits = (data["extraCredits"] as? Number)?.toInt() ?: 0,
            totalAvailableCredits = (data["totalAvailableCredits"] as? Number)?.toInt() ?: 0
        )
    }.onFailure { telemetry.recordNonFatal(it) }

    private companion object {
        const val FN_GENERATE_SKETCH = "generateSketch"
        const val FN_USER_QUOTA = "userQuota"
        const val GENERIC_ERROR = "Something went wrong. Please try again."
    }
}
