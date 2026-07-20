package com.medipro.manager.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.medipro.manager.domain.licensing.LicenseEnvironment
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class FirebaseAuthSessionProvider @Inject constructor(
    private val licenseEnvironment: LicenseEnvironment,
) {
    fun isSignedIn(): Boolean =
        !licenseEnvironment.useDevLicensing && FirebaseAuth.getInstance().currentUser != null

    suspend fun getFreshIdToken(): String? {
        if (licenseEnvironment.useDevLicensing) return null
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return suspendCancellableCoroutine { cont ->
            user.getIdToken(true)
                .addOnSuccessListener { cont.resume(it.token?.takeIf { token -> token.isNotBlank() }) }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    fun currentUid(): String? =
        if (licenseEnvironment.useDevLicensing) null else FirebaseAuth.getInstance().currentUser?.uid
}
