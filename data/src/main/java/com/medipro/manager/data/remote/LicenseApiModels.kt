package com.medipro.manager.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class CreateLicenseRequest(
    val idToken: String,
    val firebaseUid: String,
    val mobileNumber: String,
    val deviceId: String,
    val pharmacyName: String,
    val ownerName: String,
)

@Serializable
data class VerifyLicenseRequest(
    val idToken: String,
    val licenseId: String,
    val deviceId: String,
)

@Serializable
data class TransferLicenseRequest(
    val idToken: String,
    val licenseId: String,
    val newDeviceId: String,
    val confirmTransfer: Boolean,
)

@Serializable
data class LicenseApiResponse(
    val licenseId: String,
    val mobileNumber: String,
    val deviceId: String,
    val pharmacyName: String,
    val ownerName: String,
    val plan: String,
    val status: String,
    val activationDate: String,
    val expiryDate: String,
    val activationEpochMs: Long,
    val expiryEpochMs: Long,
)

interface LicenseApiClient {
    suspend fun createLicense(request: CreateLicenseRequest): Result<LicenseApiResponse>
    suspend fun verifyLicense(request: VerifyLicenseRequest): Result<LicenseApiResponse>
    suspend fun transferLicense(request: TransferLicenseRequest): Result<LicenseApiResponse>
}
