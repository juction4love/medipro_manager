package com.medipro.manager.data.remote

import com.medipro.manager.domain.licensing.LicenseEnvironment
import com.medipro.manager.domain.model.License
import com.medipro.manager.domain.model.LicensePlan
import com.medipro.manager.domain.model.LicenseStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calls Cloud Functions (HTTPS) — licenses are never written from the client to Firestore directly.
 */
@Singleton
class HttpLicenseApiClient @Inject constructor(
    private val environment: LicenseEnvironment,
) : LicenseApiClient {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun createLicense(request: CreateLicenseRequest): Result<LicenseApiResponse> =
        post("createLicense", request)

    override suspend fun verifyLicense(request: VerifyLicenseRequest): Result<LicenseApiResponse> =
        post("verifyLicense", request)

    override suspend fun transferLicense(request: TransferLicenseRequest): Result<LicenseApiResponse> =
        post("transferLicense", request)

    private suspend inline fun <reified T> post(path: String, body: T): Result<LicenseApiResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val base = environment.licenseApiBaseUrl.trimEnd('/')
                val url = URL("$base/$path")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 15_000
                    readTimeout = 15_000
                }
                conn.outputStream.use { it.write(json.encodeToString(body).toByteArray()) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val responseBody = stream?.bufferedReader()?.readText().orEmpty()
                if (code !in 200..299) error("License API error ($code): $responseBody")
                json.decodeFromString<LicenseApiResponse>(responseBody)
            }
        }
}

/**
 * Debug / offline dev licensing when Firebase backend is not configured.
 */
@Singleton
class DevLicenseApiClient @Inject constructor() : LicenseApiClient {

    override suspend fun createLicense(request: CreateLicenseRequest): Result<LicenseApiResponse> {
        val now = System.currentTimeMillis()
        val expiry = now + ONE_YEAR_MS
        return Result.success(
            LicenseApiResponse(
                licenseId = "LIC-DEV-${UUID.randomUUID().toString().take(8).uppercase()}",
                mobileNumber = request.mobileNumber,
                deviceId = request.deviceId,
                pharmacyName = request.pharmacyName,
                ownerName = request.ownerName,
                plan = LicensePlan.FREE,
                status = LicenseStatus.ACTIVE,
                activationDate = formatDate(now),
                expiryDate = formatDate(expiry),
                activationEpochMs = now,
                expiryEpochMs = expiry,
            )
        )
    }

    override suspend fun verifyLicense(request: VerifyLicenseRequest): Result<LicenseApiResponse> =
        Result.failure(UnsupportedOperationException("Dev verify uses local cache"))

    override suspend fun transferLicense(request: TransferLicenseRequest): Result<LicenseApiResponse> =
        createLicense(
            CreateLicenseRequest(
                idToken = request.idToken,
                firebaseUid = "dev",
                mobileNumber = "",
                deviceId = request.newDeviceId,
                pharmacyName = "",
                ownerName = "",
            )
        )

    private fun formatDate(epochMs: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = epochMs }
        return "${cal.get(java.util.Calendar.YEAR)}-${cal.get(java.util.Calendar.MONTH) + 1}-${cal.get(java.util.Calendar.DAY_OF_MONTH)}"
    }

    companion object {
        private const val ONE_YEAR_MS = 365L * 24 * 60 * 60 * 1000
    }
}

fun LicenseApiResponse.toDomain(): License = License(
    licenseId = licenseId,
    licenseKey = licenseId,
    mobileNumber = mobileNumber,
    pharmacyName = pharmacyName,
    ownerName = ownerName,
    deviceId = deviceId,
    plan = plan,
    status = status,
    activatedAt = activationEpochMs,
    expiresAt = expiryEpochMs,
    lastVerifiedAt = System.currentTimeMillis(),
    isActive = status == LicenseStatus.ACTIVE,
)
