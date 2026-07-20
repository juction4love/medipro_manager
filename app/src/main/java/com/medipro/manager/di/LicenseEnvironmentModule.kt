package com.medipro.manager.di

import com.medipro.manager.BuildConfig
import com.medipro.manager.domain.licensing.LicenseEnvironment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LicenseEnvironmentModule {

    @Provides
    @Singleton
    fun provideLicenseEnvironment(): LicenseEnvironment = object : LicenseEnvironment {
        override val useDevLicensing: Boolean = BuildConfig.USE_DEV_LICENSING
        override val licenseApiBaseUrl: String = BuildConfig.LICENSE_API_BASE_URL
    }
}
