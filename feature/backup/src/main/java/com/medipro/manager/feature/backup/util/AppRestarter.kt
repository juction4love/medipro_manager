package com.medipro.manager.feature.backup.util

import android.content.Context
import android.content.Intent

object AppRestarter {
    fun restart(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(launchIntent)
        Runtime.getRuntime().exit(0)
    }
}
