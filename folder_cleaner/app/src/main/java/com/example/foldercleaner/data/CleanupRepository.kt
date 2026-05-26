package com.example.foldercleaner.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.foldercleaner.work.WorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

class CleanupRepository(
    private val appContext: Context,
    private val folderDao: SelectedFolderDao,
    private val ignoredExtensionDao: IgnoredExtensionDao,
    private val cleanupRunDao: CleanupRunDao,
    private val settingsStore: SettingsStore
) {
    private companion object {
        const val MAX_HISTORY_RUNS = 100
    }

    val selectedFoldersFlow: Flow<List<SelectedFolderEntity>> = folderDao.observeAll()
    val ignoredExtensionsFlow: Flow<List<IgnoredExtensionEntity>> = ignoredExtensionDao.observeAll()
    val cleanupRunsFlow: Flow<List<CleanupRunEntity>> = cleanupRunDao.observeAll()
    val daysToKeepFlow: Flow<Int> = settingsStore.daysToKeepFlow
    val scheduleConfigFlow: Flow<ScheduleConfig> = settingsStore.scheduleConfigFlow

    suspend fun updateDaysToKeep(days: Int) {
        settingsStore.updateDaysToKeep(days)
    }

    suspend fun getScheduleConfig(): ScheduleConfig {
        return settingsStore.getScheduleConfig()
    }

    suspend fun updateScheduleConfig(config: ScheduleConfig) {
        settingsStore.updateScheduleConfig(config)
        WorkScheduler.schedulePeriodicCleanup(appContext, config)
    }

    suspend fun addFolder(uri: String, displayName: String) {
        folderDao.insert(
            SelectedFolderEntity(
                uri = uri,
                displayName = displayName,
                isEnabled = true
            )
        )
    }

    suspend fun setFolderEnabled(uri: String, isEnabled: Boolean) {
        folderDao.updateEnabled(uri, isEnabled)
    }

    suspend fun removeFolder(uri: String) {
        folderDao.deleteByUri(uri)
    }

    suspend fun addIgnoredExtension(rawExtension: String): Boolean {
        val normalized = normalizeExtension(rawExtension) ?: return false
        ignoredExtensionDao.insert(IgnoredExtensionEntity(extension = normalized))
        return true
    }

    suspend fun removeIgnoredExtension(extension: String) {
        ignoredExtensionDao.deleteByExtension(extension.lowercase(Locale.ROOT))
    }

    suspend fun writeBackupToUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val daysToKeep = settingsStore.getDaysToKeep()
        val scheduleConfig = settingsStore.getScheduleConfig()
        val folders = folderDao.getAllOnce()
        val ignoredExtensions = ignoredExtensionDao.getAllOnce().map { it.extension }

        val folderArray = JSONArray().apply {
            folders.forEach { folder ->
                put(
                    JSONObject().apply {
                        put("uri", folder.uri)
                        put("displayName", folder.displayName)
                        put("isEnabled", folder.isEnabled)
                    }
                )
            }
        }

        val ignoredArray = JSONArray().apply {
            ignoredExtensions.forEach { extension -> put(extension) }
        }

        val payload = JSONObject().apply {
            put("version", 1)
            put("exportedAtMillis", System.currentTimeMillis())
            put(
                "settings",
                JSONObject().apply {
                    put("daysToKeep", daysToKeep)
                    put(
                        "schedule",
                        JSONObject().apply {
                            put("intervalDays", scheduleConfig.intervalDays)
                            put("requiresCharging", scheduleConfig.requiresCharging)
                            put("requiresDeviceIdle", scheduleConfig.requiresDeviceIdle)
                        }
                    )
                }
            )
            put("folders", folderArray)
            put("ignoredExtensions", ignoredArray)
        }

        val output = appContext.contentResolver.openOutputStream(uri) ?: return@withContext false
        output.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(payload.toString(2))
        }
        true
    }

    suspend fun importBackupFromUri(uri: Uri): BackupImportSummary? = withContext(Dispatchers.IO) {
        val input = appContext.contentResolver.openInputStream(uri) ?: return@withContext null
        val content = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(content)

        val settings = root.optJSONObject("settings")
        val days = settings?.optInt("daysToKeep", SettingsStore.DEFAULT_DAYS_TO_KEEP)
            ?: SettingsStore.DEFAULT_DAYS_TO_KEEP
        settingsStore.updateDaysToKeep(days.coerceAtLeast(0))

        val scheduleObject = settings?.optJSONObject("schedule")
        val scheduleConfig = ScheduleConfig(
            intervalDays = scheduleObject?.optInt("intervalDays", ScheduleConfig.DEFAULT_INTERVAL_DAYS)
                ?.coerceAtLeast(1) ?: ScheduleConfig.DEFAULT_INTERVAL_DAYS,
            requiresCharging = scheduleObject?.optBoolean(
                "requiresCharging",
                ScheduleConfig.DEFAULT_REQUIRES_CHARGING
            ) ?: ScheduleConfig.DEFAULT_REQUIRES_CHARGING,
            requiresDeviceIdle = scheduleObject?.optBoolean(
                "requiresDeviceIdle",
                ScheduleConfig.DEFAULT_REQUIRES_DEVICE_IDLE
            ) ?: ScheduleConfig.DEFAULT_REQUIRES_DEVICE_IDLE
        )
        updateScheduleConfig(scheduleConfig)

        val importedFolders = mutableListOf<SelectedFolderEntity>()
        val foldersArray = root.optJSONArray("folders") ?: JSONArray()
        for (i in 0 until foldersArray.length()) {
            val folder = foldersArray.optJSONObject(i) ?: continue
            val uriText = folder.optString("uri").trim()
            if (uriText.isBlank()) {
                continue
            }
            val displayName = folder.optString("displayName", uriText).ifBlank { uriText }
            val isEnabled = folder.optBoolean("isEnabled", true)
            importedFolders += SelectedFolderEntity(
                uri = uriText,
                displayName = displayName,
                isEnabled = isEnabled
            )
        }

        val importedExtensions = mutableListOf<IgnoredExtensionEntity>()
        val extensionsArray = root.optJSONArray("ignoredExtensions") ?: JSONArray()
        for (i in 0 until extensionsArray.length()) {
            val extensionRaw = extensionsArray.optString(i)
            val normalized = normalizeExtension(extensionRaw) ?: continue
            importedExtensions += IgnoredExtensionEntity(extension = normalized)
        }

        folderDao.clearAll()
        if (importedFolders.isNotEmpty()) {
            folderDao.insertAll(importedFolders)
        }

        ignoredExtensionDao.clearAll()
        if (importedExtensions.isNotEmpty()) {
            ignoredExtensionDao.insertAll(importedExtensions.distinctBy { it.extension })
        }

        BackupImportSummary(
            folderCount = importedFolders.size,
            extensionCount = importedExtensions.distinctBy { it.extension }.size
        )
    }

    suspend fun performCleanup(trigger: CleanupTrigger): CleanupSummary = withContext(Dispatchers.IO) {
        val folders = folderDao.getAllEnabledOnce()
        val ignoredExtensions = ignoredExtensionDao.getAllOnce()
            .map { it.extension.lowercase(Locale.ROOT) }
            .toSet()
        val daysToKeep = settingsStore.getDaysToKeep()
        val cutoffMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysToKeep.toLong())

        var deletedCount = 0
        var skippedCount = 0
        var failedCount = 0
        var scannedCount = 0
        var reclaimedBytes = 0L

        folders.forEach { folder ->
            val treeUri = Uri.parse(folder.uri)
            val root = DocumentFile.fromTreeUri(appContext, treeUri) ?: return@forEach
            val children = try {
                root.listFiles()
            } catch (_: SecurityException) {
                emptyArray()
            }

            children.forEach { document ->
                if (!document.isFile) {
                    return@forEach
                }

                scannedCount += 1

                val name = document.name.orEmpty()
                if (isIgnored(name, ignoredExtensions)) {
                    skippedCount += 1
                    return@forEach
                }

                val lastModified = document.lastModified()
                if (lastModified <= 0L) {
                    skippedCount += 1
                    return@forEach
                }

                if (lastModified >= cutoffMillis) {
                    skippedCount += 1
                    return@forEach
                }

                val fileSizeBytes = document.length().coerceAtLeast(0L)

                val deleted = try {
                    document.delete()
                } catch (_: SecurityException) {
                    false
                }

                if (deleted) {
                    deletedCount += 1
                    reclaimedBytes += fileSizeBytes
                } else {
                    failedCount += 1
                }
            }
        }

        val summary = CleanupSummary(
            scannedCount = scannedCount,
            deletedCount = deletedCount,
            skippedCount = skippedCount,
            failedCount = failedCount,
            reclaimedBytes = reclaimedBytes
        )

        cleanupRunDao.insert(
            CleanupRunEntity(
                executedAtMillis = System.currentTimeMillis(),
                trigger = trigger.value,
                scannedCount = summary.scannedCount,
                deletedCount = summary.deletedCount,
                skippedCount = summary.skippedCount,
                failedCount = summary.failedCount,
                reclaimedBytes = summary.reclaimedBytes
            )
        )
        cleanupRunDao.pruneToLatest(MAX_HISTORY_RUNS)

        summary
    }

    suspend fun performAutomaticCleanupIfDue(minGapMillis: Long): CleanupSummary? {
        val safeGapMillis = minGapMillis.coerceAtLeast(0L)
        val lastAutomaticRunMillis = cleanupRunDao.getLatestExecutionMillis(CleanupTrigger.Automatic.value)
        val now = System.currentTimeMillis()

        if (lastAutomaticRunMillis != null && now - lastAutomaticRunMillis < safeGapMillis) {
            return null
        }

        return performCleanup(CleanupTrigger.Automatic)
    }

    private fun isIgnored(fileName: String, ignoredExtensions: Set<String>): Boolean {
        val lastDotIndex = fileName.lastIndexOf('.')
        if (lastDotIndex < 0 || lastDotIndex == fileName.length - 1) {
            return false
        }
        val extension = fileName.substring(lastDotIndex).lowercase(Locale.ROOT)
        return extension in ignoredExtensions
    }

    private fun normalizeExtension(rawExtension: String): String? {
        val clean = rawExtension.trim().lowercase(Locale.ROOT)
        if (clean.isBlank()) {
            return null
        }
        val startsWithDot = if (clean.startsWith('.')) clean else ".$clean"
        return if (startsWithDot.length > 1) startsWithDot else null
    }
}

enum class CleanupTrigger(val value: String) {
    Automatic("automatic"),
    Manual("manual")
}

data class CleanupSummary(
    val scannedCount: Int,
    val deletedCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
    val reclaimedBytes: Long
)

data class BackupImportSummary(
    val folderCount: Int,
    val extensionCount: Int
)
