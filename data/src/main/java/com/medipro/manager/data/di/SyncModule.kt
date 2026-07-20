package com.medipro.manager.data.di

import com.google.firebase.firestore.FirebaseFirestore
import com.medipro.manager.data.sync.ApplicationScope
import com.medipro.manager.data.sync.FirestoreSyncRepositoryImpl
import com.medipro.manager.data.sync.FirestoreProvider
import com.medipro.manager.domain.repository.FirestoreSyncRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    @Singleton
    abstract fun bindFirestoreSyncRepository(impl: FirestoreSyncRepositoryImpl): FirestoreSyncRepository
}

@Module
@InstallIn(SingletonComponent::class)
object FirestoreModule {
    @Provides
    @Singleton
    fun provideFirestore(provider: FirestoreProvider): FirebaseFirestore = provider.firestore()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
