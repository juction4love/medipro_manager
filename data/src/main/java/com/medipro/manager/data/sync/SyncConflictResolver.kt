package com.medipro.manager.data.sync

/**
 * Conflict resolution: syncVersion → updatedAt → deviceId duplicate ignore.
 */
object SyncConflictResolver {

    fun shouldApplyRemote(
        localVersion: Long,
        localUpdatedAt: Long,
        localDeviceId: String?,
        remoteVersion: Long,
        remoteUpdatedAt: Long,
        remoteDeviceId: String?,
    ): Boolean {
        if (remoteVersion > localVersion) return true
        if (remoteVersion < localVersion) return false
        if (remoteUpdatedAt > localUpdatedAt) return true
        if (remoteUpdatedAt < localUpdatedAt) return false
        if (remoteDeviceId != null && remoteDeviceId == localDeviceId) return false
        return true
    }

    fun shouldSkipExisting(
        localVersion: Long,
        localUpdatedAt: Long,
        localDeviceId: String?,
        remoteVersion: Long,
        remoteUpdatedAt: Long,
        remoteDeviceId: String?,
    ): Boolean = !shouldApplyRemote(
        localVersion = localVersion,
        localUpdatedAt = localUpdatedAt,
        localDeviceId = localDeviceId,
        remoteVersion = remoteVersion,
        remoteUpdatedAt = remoteUpdatedAt,
        remoteDeviceId = remoteDeviceId,
    )
}
