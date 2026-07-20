package com.medipro.manager.feature.license.auth

import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

object PhoneAuthErrorMapper {
    fun message(error: Throwable?): String {
        if (error == null) return "Authentication failed"
        return when (error) {
            is FirebaseAuthInvalidCredentialsException -> "Invalid OTP. Check the code and try again."
            is FirebaseAuthInvalidUserException -> "This phone number is not valid for sign-in."
            is FirebaseAuthException -> when (error.errorCode) {
                "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Wait a few minutes and try again."
                "ERROR_INVALID_VERIFICATION_CODE" -> "Invalid OTP. Check the code and try again."
                "ERROR_SESSION_EXPIRED" -> "OTP expired. Tap Resend OTP and try again."
                else -> error.message ?: "Phone verification failed"
            }
            is FirebaseException -> when {
                error.message?.contains("quota", ignoreCase = true) == true ->
                    "SMS quota exceeded. Try again later or contact support."
                error.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Check internet connection and retry."
                error.message?.contains("invalid phone", ignoreCase = true) == true ->
                    "Invalid phone number. Enter a 10-digit Nepal mobile number."
                else -> error.message ?: "Phone verification failed"
            }
            else -> error.message ?: "Authentication failed"
        }
    }
}
