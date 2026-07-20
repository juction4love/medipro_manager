package com.medipro.manager.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class StoredPrinterProfile {
    COUNTER,
    OFFICE,
}

data class StoredPrinterSettings(
    val printerName: String = "",
    val macAddress: String = "",
    val paperWidthMm: Int = 58,
    val charsPerLine: Int = 32,
    val autoConnect: Boolean = true,
    val autoCut: Boolean = true,
    val openCashDrawer: Boolean = false,
    val printLogo: Boolean = true,
    val printDuplicateCopy: Boolean = false,
)

private val Context.printerDataStore: DataStore<Preferences> by preferencesDataStore("printer_prefs")

@Singleton
class PrinterPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val legacyNameKey = stringPreferencesKey("printer_name")
    private val legacyMacKey = stringPreferencesKey("mac_address")
    private val legacyPaperWidthKey = intPreferencesKey("paper_width_mm")
    private val legacyCharsPerLineKey = intPreferencesKey("chars_per_line")
    private val legacyAutoConnectKey = booleanPreferencesKey("auto_connect")
    private val legacyAutoCutKey = booleanPreferencesKey("auto_cut")
    private val legacyOpenDrawerKey = booleanPreferencesKey("open_cash_drawer")
    private val legacyPrintLogoKey = booleanPreferencesKey("print_logo")
    private val legacyDuplicateCopyKey = booleanPreferencesKey("print_duplicate")

    private fun nameKey(profile: StoredPrinterProfile) =
        stringPreferencesKey("${profile.name.lowercase()}_printer_name")
    private fun macKey(profile: StoredPrinterProfile) =
        stringPreferencesKey("${profile.name.lowercase()}_mac_address")
    private fun paperWidthKey(profile: StoredPrinterProfile) =
        intPreferencesKey("${profile.name.lowercase()}_paper_width_mm")
    private fun charsPerLineKey(profile: StoredPrinterProfile) =
        intPreferencesKey("${profile.name.lowercase()}_chars_per_line")
    private fun autoConnectKey(profile: StoredPrinterProfile) =
        booleanPreferencesKey("${profile.name.lowercase()}_auto_connect")
    private fun autoCutKey(profile: StoredPrinterProfile) =
        booleanPreferencesKey("${profile.name.lowercase()}_auto_cut")
    private fun openDrawerKey(profile: StoredPrinterProfile) =
        booleanPreferencesKey("${profile.name.lowercase()}_open_cash_drawer")
    private fun printLogoKey(profile: StoredPrinterProfile) =
        booleanPreferencesKey("${profile.name.lowercase()}_print_logo")
    private fun duplicateCopyKey(profile: StoredPrinterProfile) =
        booleanPreferencesKey("${profile.name.lowercase()}_print_duplicate")

    fun settings(profile: StoredPrinterProfile): Flow<StoredPrinterSettings> =
        context.printerDataStore.data.map { prefs -> readProfile(prefs, profile) }

    val settings: Flow<StoredPrinterSettings> = settings(StoredPrinterProfile.COUNTER)

    suspend fun get(profile: StoredPrinterProfile = StoredPrinterProfile.COUNTER): StoredPrinterSettings =
        settings(profile).first()

    suspend fun get(): StoredPrinterSettings = get(StoredPrinterProfile.COUNTER)

    suspend fun save(profile: StoredPrinterProfile, settings: StoredPrinterSettings) {
        context.printerDataStore.edit { prefs ->
            prefs[nameKey(profile)] = settings.printerName
            prefs[macKey(profile)] = settings.macAddress
            prefs[paperWidthKey(profile)] = settings.paperWidthMm
            prefs[charsPerLineKey(profile)] = settings.charsPerLine
            prefs[autoConnectKey(profile)] = settings.autoConnect
            prefs[autoCutKey(profile)] = settings.autoCut
            prefs[openDrawerKey(profile)] = settings.openCashDrawer
            prefs[printLogoKey(profile)] = settings.printLogo
            prefs[duplicateCopyKey(profile)] = settings.printDuplicateCopy
        }
    }

    suspend fun save(settings: StoredPrinterSettings) = save(StoredPrinterProfile.COUNTER, settings)

    private fun readProfile(prefs: Preferences, profile: StoredPrinterProfile): StoredPrinterSettings {
        val profileMac = prefs[macKey(profile)].orEmpty()
        val legacyMac = prefs[legacyMacKey].orEmpty()
        val useLegacy = profile == StoredPrinterProfile.COUNTER && profileMac.isBlank() && legacyMac.isNotBlank()
        return StoredPrinterSettings(
            printerName = if (useLegacy) prefs[legacyNameKey].orEmpty() else prefs[nameKey(profile)].orEmpty(),
            macAddress = if (useLegacy) legacyMac else profileMac,
            paperWidthMm = if (useLegacy) prefs[legacyPaperWidthKey] ?: 58 else prefs[paperWidthKey(profile)] ?: 58,
            charsPerLine = if (useLegacy) prefs[legacyCharsPerLineKey] ?: 32 else prefs[charsPerLineKey(profile)] ?: 32,
            autoConnect = if (useLegacy) prefs[legacyAutoConnectKey] ?: true else prefs[autoConnectKey(profile)] ?: true,
            autoCut = if (useLegacy) prefs[legacyAutoCutKey] ?: true else prefs[autoCutKey(profile)] ?: true,
            openCashDrawer = if (useLegacy) prefs[legacyOpenDrawerKey] ?: false else prefs[openDrawerKey(profile)] ?: false,
            printLogo = if (useLegacy) prefs[legacyPrintLogoKey] ?: true else prefs[printLogoKey(profile)] ?: true,
            printDuplicateCopy = if (useLegacy) prefs[legacyDuplicateCopyKey] ?: false else prefs[duplicateCopyKey(profile)] ?: false,
        )
    }
}
