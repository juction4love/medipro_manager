package com.medipro.manager.data.di

import com.medipro.manager.data.remote.DevLicenseApiClient
import com.medipro.manager.data.remote.HttpLicenseApiClient
import com.medipro.manager.data.remote.LicenseApiClient
import com.medipro.manager.domain.licensing.LicenseEnvironment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LicenseApiModule {

    @Provides
    @Singleton
    fun provideLicenseApiClient(
        environment: LicenseEnvironment,
        devClient: DevLicenseApiClient,
        httpClient: HttpLicenseApiClient,
    ): LicenseApiClient = if (environment.useDevLicensing) devClient else httpClient
}
