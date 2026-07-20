package com.medipro.manager.data.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncConflictResolverTest {

    @Test
    fun remoteHigherSyncVersion_wins() {
        assertTrue(
            SyncConflictResolver.shouldApplyRemote(
                localVersion = 1,
                localUpdatedAt = 1000L,
                localDeviceId = "device-a",
                remoteVersion = 2,
                remoteUpdatedAt = 900L,
                remoteDeviceId = "device-b",
            )
        )
    }

    @Test
    fun localHigherSyncVersion_skipsRemote() {
        assertFalse(
            SyncConflictResolver.shouldApplyRemote(
                localVersion = 3,
                localUpdatedAt = 1000L,
                localDeviceId = "device-a",
                remoteVersion = 2,
                remoteUpdatedAt = 2000L,
                remoteDeviceId = "device-b",
            )
        )
    }

    @Test
    fun equalVersion_newerUpdatedAt_wins() {
        assertTrue(
            SyncConflictResolver.shouldApplyRemote(
                localVersion = 2,
                localUpdatedAt = 1000L,
                localDeviceId = "device-a",
                remoteVersion = 2,
                remoteUpdatedAt = 1500L,
                remoteDeviceId = "device-b",
            )
        )
    }

    @Test
    fun equalVersion_olderUpdatedAt_skipsRemote() {
        assertFalse(
            SyncConflictResolver.shouldApplyRemote(
                localVersion = 2,
                localUpdatedAt = 2000L,
                localDeviceId = "device-a",
                remoteVersion = 2,
                remoteUpdatedAt = 1500L,
                remoteDeviceId = "device-b",
            )
        )
    }

    @Test
    fun equalVersionAndTimestamp_sameDevice_skipsDuplicateEcho() {
        assertFalse(
            SyncConflictResolver.shouldApplyRemote(
                localVersion = 2,
                localUpdatedAt = 1000L,
                localDeviceId = "device-a",
                remoteVersion = 2,
                remoteUpdatedAt = 1000L,
                remoteDeviceId = "device-a",
            )
        )
    }

    @Test
    fun equalVersionAndTimestamp_differentDevice_appliesRemote() {
        assertTrue(
            SyncConflictResolver.shouldApplyRemote(
                localVersion = 2,
                localUpdatedAt = 1000L,
                localDeviceId = "device-a",
                remoteVersion = 2,
                remoteUpdatedAt = 1000L,
                remoteDeviceId = "device-b",
            )
        )
    }

    @Test
    fun shouldSkipExisting_isInverseOfShouldApplyRemote() {
        val apply = SyncConflictResolver.shouldApplyRemote(
            localVersion = 2,
            localUpdatedAt = 1000L,
            localDeviceId = "device-a",
            remoteVersion = 2,
            remoteUpdatedAt = 1000L,
            remoteDeviceId = "device-b",
        )
        val skip = SyncConflictResolver.shouldSkipExisting(
            localVersion = 2,
            localUpdatedAt = 1000L,
            localDeviceId = "device-a",
            remoteVersion = 2,
            remoteUpdatedAt = 1000L,
            remoteDeviceId = "device-b",
        )
        assertTrue(apply != skip)
    }
}
