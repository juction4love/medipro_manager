package com.medipro.manager.data.di

import com.medipro.manager.data.licensing.LicenseManagerImpl
import com.medipro.manager.domain.licensing.LicenseManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LicenseManagerModule {

    @Binds
    @Singleton
    abstract fun bindLicenseManager(impl: LicenseManagerImpl): LicenseManager
}
