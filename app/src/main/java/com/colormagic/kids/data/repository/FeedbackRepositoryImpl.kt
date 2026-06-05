package com.colormagic.kids.data.repository

import android.os.Build
import com.colormagic.kids.BuildConfig
import com.colormagic.kids.data.telemetry.AppTelemetry
import com.colormagic.kids.domain.repository.FeedbackRepository
import com.colormagic.kids.domain.repository.FeedbackType
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions,
    private val telemetry: AppTelemetry
) : FeedbackRepository {

    override suspend fun submit(
        type: FeedbackType,
        message: String,
        email: String?
    ): Result<Unit> = runCatching {
        val payload = mapOf(
            "type" to type.wireValue,
            "message" to message.trim(),
            "email" to (email?.trim().orEmpty()),
            // Diagnostics help triage bug reports — no PII.
            "appVersion" to "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "platform" to "Android ${Build.VERSION.RELEASE}"
        )
        functions.getHttpsCallable(FN_SUBMIT_FEEDBACK).call(payload).await()
        Unit
    }.onFailure { telemetry.recordNonFatal(it) }

    private companion object {
        const val FN_SUBMIT_FEEDBACK = "submitFeedback"
    }
}
