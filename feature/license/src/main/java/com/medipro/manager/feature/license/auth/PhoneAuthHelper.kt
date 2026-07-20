package com.medipro.manager.feature.license.auth

import android.app.Activity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.medipro.manager.domain.licensing.LicenseEnvironment
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class PhoneAuthHelper @Inject constructor(
    private val licenseEnvironment: LicenseEnvironment,
) {

    private val auth: FirebaseAuth? by lazy {
        runCatching { FirebaseAuth.getInstance() }.getOrNull()
    }

    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var lastFormattedNumber: String? = null

    fun isFirebaseAvailable(): Boolean =
        !licenseEnvironment.useDevLicensing && runCatching { FirebaseApp.getInstance() }.isSuccess

    suspend fun sendOtp(mobileNumber: String, activity: Activity): Result<OtpSendResult> {
        if (licenseEnvironment.useDevLicensing || !isFirebaseAvailable()) {
            return Result.success(OtpSendResult(verificationId = DEV_VERIFICATION_ID))
        }
        val firebaseAuth = auth ?: return Result.failure(IllegalStateException("Firebase Auth unavailable"))
        val formatted = formatNepalNumber(mobileNumber)
        lastFormattedNumber = formatted
        return suspendCancellableCoroutine { cont ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    firebaseAuth.signInWithCredential(credential)
                        .addOnSuccessListener {
                            cont.resume(Result.success(OtpSendResult(verificationId = AUTO_VERIFIED_ID, autoVerified = true)))
                        }
                        .addOnFailureListener { cont.resume(Result.failure(it)) }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    cont.resume(Result.failure(e))
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    resendToken = token
                    cont.resume(Result.success(OtpSendResult(verificationId = verificationId)))
                }
            }
            PhoneAuthProvider.verifyPhoneNumber(
                PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber(formatted)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                    .build(),
            )
        }
    }

    suspend fun resendOtp(mobileNumber: String, activity: Activity): Result<OtpSendResult> {
        if (licenseEnvironment.useDevLicensing || !isFirebaseAvailable()) {
            return Result.success(OtpSendResult(verificationId = DEV_VERIFICATION_ID))
        }
        val firebaseAuth = auth ?: return Result.failure(IllegalStateException("Firebase Auth unavailable"))
        val formatted = formatNepalNumber(mobileNumber)
        lastFormattedNumber = formatted
        val token = resendToken
        return suspendCancellableCoroutine { cont ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    firebaseAuth.signInWithCredential(credential)
                        .addOnSuccessListener {
                            cont.resume(Result.success(OtpSendResult(verificationId = AUTO_VERIFIED_ID, autoVerified = true)))
                        }
                        .addOnFailureListener { cont.resume(Result.failure(it)) }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    cont.resume(Result.failure(e))
                }

                override fun onCodeSent(verificationId: String, newToken: PhoneAuthProvider.ForceResendingToken) {
                    resendToken = newToken
                    cont.resume(Result.success(OtpSendResult(verificationId = verificationId)))
                }
            }
            val builder = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(formatted)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
            if (token != null) {
                builder.setForceResendingToken(token)
            }
            PhoneAuthProvider.verifyPhoneNumber(builder.build())
        }
    }

    suspend fun verifyOtp(verificationId: String, otp: String): Result<AuthResult> {
        if (licenseEnvironment.useDevLicensing || verificationId == DEV_VERIFICATION_ID) {
            if (otp.length < 4) return Result.failure(IllegalArgumentException("Invalid OTP"))
            return Result.success(AuthResult(firebaseUid = "dev-uid", idToken = "dev-token"))
        }
        if (verificationId == AUTO_VERIFIED_ID) {
            return getCurrentAuthResult()
        }
        val firebaseAuth = auth ?: return Result.failure(IllegalStateException("Firebase Auth unavailable"))
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        return suspendCancellableCoroutine { cont ->
            firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener {
                    val user = firebaseAuth.currentUser
                    if (user == null) {
                        cont.resume(Result.failure(IllegalStateException("Not signed in")))
                        return@addOnSuccessListener
                    }
                    user.getIdToken(true)
                        .addOnSuccessListener { tokenResult ->
                            cont.resume(
                                Result.success(
                                    AuthResult(user.uid, tokenResult.token.orEmpty()),
                                ),
                            )
                        }
                        .addOnFailureListener { cont.resume(Result.failure(it)) }
                }
                .addOnFailureListener { cont.resume(Result.failure(it)) }
        }
    }

    suspend fun getCurrentAuthResult(): Result<AuthResult> {
        if (licenseEnvironment.useDevLicensing) {
            return Result.success(AuthResult(firebaseUid = "dev-uid", idToken = "dev-token"))
        }
        val user = auth?.currentUser ?: return Result.failure(IllegalStateException("Session expired. Verify OTP again."))
        return suspendCancellableCoroutine { cont ->
            user.getIdToken(true)
                .addOnSuccessListener { tokenResult ->
                    val token = tokenResult.token.orEmpty()
                    if (token.isBlank()) {
                        cont.resume(Result.failure(IllegalStateException("Could not refresh sign-in token")))
                    } else {
                        cont.resume(Result.success(AuthResult(user.uid, token)))
                    }
                }
                .addOnFailureListener { cont.resume(Result.failure(it)) }
        }
    }

    fun clearSessionState() {
        resendToken = null
        lastFormattedNumber = null
    }

    private fun formatNepalNumber(number: String): String {
        val digits = number.filter { it.isDigit() }
        return when {
            digits.startsWith("977") -> "+$digits"
            digits.length == 10 -> "+977$digits"
            number.startsWith("+") -> number
            else -> "+977$digits"
        }
    }

    data class AuthResult(val firebaseUid: String, val idToken: String)

    data class OtpSendResult(
        val verificationId: String,
        val autoVerified: Boolean = false,
    )

    companion object {
        const val DEV_VERIFICATION_ID = "dev-verification-id"
        const val AUTO_VERIFIED_ID = "auto-verified"
    }
}
