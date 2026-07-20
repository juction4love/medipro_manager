package com.medipro.manager.domain.licensing

interface LicenseEnvironment {
    val useDevLicensing: Boolean
    val licenseApiBaseUrl: String
}
