package com.example.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.StatFs
import com.example.data.local.AppDatabase
import com.example.data.preferences.ShopPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

enum class BackupKind {
    AUTO,
    MANUAL,
    SAFETY
}

@Serializable
data class BackupManifest(
    val formatVersion: Int = 1,
    val schemaVersion: Int,
    val createdAtMillis: Long,
    val kind: String,
    val dbSha256: String,
    val dbSizeBytes: Long,
    val customerCount: Int,
    val measurementCount: Int,
    val orderCount: Int,
    val paymentCount: Int,
    val productCount: Int,
    val expenseCount: Int
)

data class LocalBackupEntry(
    val file: File,
    val fileName: String,
    val kind: BackupKind,
    val createdAtMillis: Long,
    val sizeBytes: Long,
    val manifest: BackupManifest?
)

data class RestorePreviewData(
    val sourceLabel: String,
    val localFile: File?,
    val stagedDbFile: File,
    val manifest: BackupManifest,
    val checksumVerified: Boolean,
    val sqliteIntegrityOk: Boolean,
    val customerCount: Int,
    val measurementCount: Int,
    val orderCount: Int,
    val paymentCount: Int,
    val productCount: Int,
    val expenseCount: Int
)

sealed class BackupInspectResult {
    data class Valid(val preview: RestorePreviewData) : BackupInspectResult()
    data class Corrupted(val reason: String) : BackupInspectResult()
}

class BackupManager(
    private val context: Context,
    private val prefsRepo: ShopPreferencesRepository
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    private val mutex = Mutex()

    private val _databaseEpoch = MutableStateFlow(0)
    val databaseEpoch: StateFlow<Int> = _databaseEpoch.asStateFlow()

    private val backupsRootDir: File
        get() = File(context.filesDir, "backups").apply { mkdirs() }

    private val stagingDir: File
        get() = File(context.cacheDir, "backup_staging").apply { mkdirs() }

    fun getAvailableStorageBytes(): Long {
        return try {
            val stat = StatFs(context.filesDir.path)
            val statBytes = stat.availableBytes
            if (statBytes > 0L) statBytes else context.filesDir.usableSpace
        } catch (_: Exception) {
            context.filesDir.usableSpace
        }
    }

    fun isLowStorageWarning(): Boolean {
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        val minNeeded = maxOf(25L * 1024L * 1024L, (dbFile.length().coerceAtLeast(1024L)) * 3L)
        return getAvailableStorageBytes() < minNeeded
    }

    suspend fun createLocalBackup(kind: BackupKind): Result<LocalBackupEntry> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val result = createBackupInternal(kind)
            if (result.isSuccess && kind != BackupKind.SAFETY) {
                prefsRepo.recordBackupOutcome(
                    timestamp = System.currentTimeMillis(),
                    success = true,
                    errorMessage = ""
                )
            } else if (result.isFailure && kind != BackupKind.SAFETY) {
                prefsRepo.recordBackupOutcome(
                    timestamp = System.currentTimeMillis(),
                    success = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Backup failed"
                )
            }
            result
        }
    }

    private fun createBackupInternal(kind: BackupKind): Result<LocalBackupEntry> {
        return runCatching {
            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val minSafeBytes = (dbFile.length().coerceAtLeast(64L * 1024L)) * 2L + (1024L * 1024L)
            if (getAvailableStorageBytes() < minSafeBytes) {
                throw IllegalStateException("Insufficient storage space to safely create backup")
            }

            // Ensure database exists and checkpoint WAL cleanly
            val db = AppDatabase.getInstance(context)
            AppDatabase.checkpointWal(context)

            val tempDbCopy = File(stagingDir, "snapshot_${System.currentTimeMillis()}.db")
            try {
                if (!dbFile.exists()) {
                    throw IllegalStateException("Database file not initialized yet")
                }
                dbFile.copyTo(tempDbCopy, overwrite = true)

                // Verify integrity of snapshot before packaging
                if (!verifySqliteIntegrity(tempDbCopy)) {
                    throw IllegalStateException("SQLite integrity check failed on snapshot")
                }

                val counts = readTableCounts(tempDbCopy)
                val sha256 = computeSha256(tempDbCopy)
                val now = System.currentTimeMillis()
                val manifest = BackupManifest(
                    formatVersion = 1,
                    schemaVersion = db.openHelper.readableDatabase.version,
                    createdAtMillis = now,
                    kind = kind.name,
                    dbSha256 = sha256,
                    dbSizeBytes = tempDbCopy.length(),
                    customerCount = counts.customers,
                    measurementCount = counts.measurements,
                    orderCount = counts.orders,
                    paymentCount = counts.payments,
                    productCount = counts.products,
                    expenseCount = counts.expenses
                )

                val targetSubdir = File(backupsRootDir, kind.name.lowercase(Locale.US)).apply { mkdirs() }
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date(now))
                val targetZip = File(targetSubdir, "azad_taillors_${kind.name.lowercase(Locale.US)}_$stamp.atbackup")
                val tempZip = File(targetSubdir, ".tmp_${targetZip.name}")

                ZipOutputStream(BufferedOutputStream(FileOutputStream(tempZip))).use { zos ->
                    zos.putNextEntry(ZipEntry("manifest.json"))
                    zos.write(json.encodeToString(manifest).toByteArray(Charsets.UTF_8))
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry(AppDatabase.DATABASE_NAME))
                    FileInputStream(tempDbCopy).use { fis ->
                        fis.copyTo(zos, bufferSize = 16 * 1024)
                    }
                    zos.closeEntry()
                }

                if (!tempZip.renameTo(targetZip)) {
                    tempZip.copyTo(targetZip, overwrite = true)
                    tempZip.delete()
                }

                // Prune older generations safely (never delete the only valid backup!)
                pruneOldBackups(targetSubdir, maxToKeep = if (kind == BackupKind.AUTO) 7 else 5)

                LocalBackupEntry(
                    file = targetZip,
                    fileName = targetZip.name,
                    kind = kind,
                    createdAtMillis = now,
                    sizeBytes = targetZip.length(),
                    manifest = manifest
                )
            } finally {
                tempDbCopy.delete()
            }
        }
    }

    suspend fun exportBackupToUri(destinationUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val created = createBackupInternal(BackupKind.MANUAL).getOrThrow()
                context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                    FileInputStream(created.file).use { input ->
                        input.copyTo(out, bufferSize = 16 * 1024)
                    }
                } ?: throw IllegalStateException("Unable to open destination file for export")

                prefsRepo.recordBackupOutcome(
                    timestamp = System.currentTimeMillis(),
                    success = true,
                    errorMessage = ""
                )
            }.onFailure { err ->
                prefsRepo.recordBackupOutcome(
                    timestamp = System.currentTimeMillis(),
                    success = false,
                    errorMessage = err.message ?: "Export failed"
                )
            }
        }
    }

    suspend fun listLocalBackups(): List<LocalBackupEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<LocalBackupEntry>()
        BackupKind.entries.forEach { kind ->
            val dir = File(backupsRootDir, kind.name.lowercase(Locale.US))
            if (dir.exists()) {
                dir.listFiles { f -> f.isFile && f.name.endsWith(".atbackup") }?.forEach { file ->
                    val manifest = readManifestQuietly(file)
                    entries.add(
                        LocalBackupEntry(
                            file = file,
                            fileName = file.name,
                            kind = kind,
                            createdAtMillis = manifest?.createdAtMillis ?: file.lastModified(),
                            sizeBytes = file.length(),
                            manifest = manifest
                        )
                    )
                }
            }
        }
        entries.sortedByDescending { it.createdAtMillis }
    }

    suspend fun deleteLocalBackup(entry: LocalBackupEntry): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val allValid = listLocalBackups().filter { it.manifest != null }
                if (allValid.size <= 1 && allValid.any { it.file.absolutePath == entry.file.absolutePath }) {
                    throw IllegalStateException("Cannot delete the only remaining valid local backup")
                }
                if (!entry.file.delete()) {
                    throw IllegalStateException("Could not delete backup file")
                }
            }
        }
    }

    suspend fun inspectBackupFromUri(uri: Uri, displayName: String): BackupInspectResult =
        withContext(Dispatchers.IO) {
            val stagedZip = File(stagingDir, "import_${System.currentTimeMillis()}.atbackup")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedOutputStream(FileOutputStream(stagedZip)).use { output ->
                        input.copyTo(output, bufferSize = 16 * 1024)
                    }
                } ?: return@withContext BackupInspectResult.Corrupted("Unable to read selected file")

                inspectZipBackupFile(
                    zipFile = stagedZip,
                    sourceLabel = displayName,
                    localFile = null
                )
            } catch (e: Exception) {
                BackupInspectResult.Corrupted(e.message ?: "Invalid or unreadable backup file")
            } finally {
                stagedZip.delete()
            }
        }

    suspend fun inspectLocalBackup(entry: LocalBackupEntry): BackupInspectResult =
        withContext(Dispatchers.IO) {
            inspectZipBackupFile(
                zipFile = entry.file,
                sourceLabel = entry.fileName,
                localFile = entry.file
            )
        }

    private fun inspectZipBackupFile(
        zipFile: File,
        sourceLabel: String,
        localFile: File?
    ): BackupInspectResult {
        if (!zipFile.exists() || zipFile.length() == 0L) {
            return BackupInspectResult.Corrupted("Backup file is empty or missing")
        }

        val stagedDb = File(stagingDir, "preview_db_${System.currentTimeMillis()}.db")
        return try {
            var manifest: BackupManifest? = null
            var extractedDb = false

            ZipFile(zipFile).use { zip ->
                val manifestEntry = zip.getEntry("manifest.json")
                    ?: return BackupInspectResult.Corrupted("Missing manifest.json inside backup archive")
                val dbEntry = zip.getEntry(AppDatabase.DATABASE_NAME)
                    ?: return BackupInspectResult.Corrupted("Missing SQLite database inside backup archive")

                val manifestText = zip.getInputStream(manifestEntry).bufferedReader(Charsets.UTF_8).readText()
                manifest = json.decodeFromString<BackupManifest>(manifestText)

                BufferedOutputStream(FileOutputStream(stagedDb)).use { out ->
                    BufferedInputStream(zip.getInputStream(dbEntry)).use { input ->
                        input.copyTo(out, bufferSize = 16 * 1024)
                    }
                }
                extractedDb = true
            }

            val parsedManifest = manifest
                ?: return BackupInspectResult.Corrupted("Corrupted backup manifest")
            if (!extractedDb || !stagedDb.exists()) {
                return BackupInspectResult.Corrupted("Failed to extract SQLite database")
            }

            if (parsedManifest.schemaVersion > AppDatabase.CURRENT_SCHEMA_VERSION) {
                return BackupInspectResult.Corrupted(
                    "Backup schema version (${parsedManifest.schemaVersion}) is newer than this app (${AppDatabase.CURRENT_SCHEMA_VERSION})"
                )
            }

            if (!hasValidSqliteHeader(stagedDb)) {
                stagedDb.delete()
                return BackupInspectResult.Corrupted("File is not a valid SQLite 3 database")
            }

            val actualSha = computeSha256(stagedDb)
            if (!actualSha.equals(parsedManifest.dbSha256, ignoreCase = true)) {
                stagedDb.delete()
                return BackupInspectResult.Corrupted("SHA-256 checksum mismatch (backup may be corrupted or modified)")
            }

            if (!verifySqliteIntegrity(stagedDb)) {
                stagedDb.delete()
                return BackupInspectResult.Corrupted("SQLite PRAGMA integrity_check failed")
            }

            val counts = readTableCounts(stagedDb)
            BackupInspectResult.Valid(
                RestorePreviewData(
                    sourceLabel = sourceLabel,
                    localFile = localFile,
                    stagedDbFile = stagedDb,
                    manifest = parsedManifest,
                    checksumVerified = true,
                    sqliteIntegrityOk = true,
                    customerCount = counts.customers,
                    measurementCount = counts.measurements,
                    orderCount = counts.orders,
                    paymentCount = counts.payments,
                    productCount = counts.products,
                    expenseCount = counts.expenses
                )
            )
        } catch (e: Exception) {
            stagedDb.delete()
            BackupInspectResult.Corrupted(e.message ?: "Corrupted backup archive")
        }
    }

    /**
     * Restores a validated backup preview:
     * 1. Re-verifies staged SQLite file integrity & checksum.
     * 2. Creates a Safety Backup of the current live database before touching anything.
     * 3. Closes Room cleanly, replaces live database file & clears WAL/SHM sidecars.
     * 4. Re-initializes AppDatabase and bumps databaseEpoch so UI refreshes cleanly.
     */
    suspend fun performRestore(preview: RestorePreviewData): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val stagedDb = preview.stagedDbFile
                if (!stagedDb.exists() || !hasValidSqliteHeader(stagedDb) || !verifySqliteIntegrity(stagedDb)) {
                    throw IllegalStateException("Staged backup failed final integrity verification")
                }

                val actualSha = computeSha256(stagedDb)
                if (!actualSha.equals(preview.manifest.dbSha256, ignoreCase = true)) {
                    throw IllegalStateException("Staged backup checksum mismatch")
                }

                // Step 1: Mandatory safety backup before overwriting live database
                val currentDbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
                if (currentDbFile.exists()) {
                    createBackupInternal(BackupKind.SAFETY).getOrThrow()
                }

                // Step 2: Close Room cleanly
                AppDatabase.closeForRestore()

                // Step 3: Remove WAL/SHM sidecars and atomically replace main .db
                File("${currentDbFile.absolutePath}-wal").delete()
                File("${currentDbFile.absolutePath}-shm").delete()
                File("${currentDbFile.absolutePath}-journal").delete()

                currentDbFile.parentFile?.mkdirs()
                val tempLive = File(currentDbFile.parentFile, "${AppDatabase.DATABASE_NAME}.restoring")
                stagedDb.copyTo(tempLive, overwrite = true)
                if (currentDbFile.exists()) {
                    currentDbFile.delete()
                }
                if (!tempLive.renameTo(currentDbFile)) {
                    tempLive.copyTo(currentDbFile, overwrite = true)
                    tempLive.delete()
                }

                // Step 4: Reopen Room to validate restored DB and trigger reactive refresh
                AppDatabase.getInstance(context).openHelper.writableDatabase
                stagedDb.delete()
                _databaseEpoch.value = _databaseEpoch.value + 1
            }
        }
    }

    fun cleanupStagedPreview(preview: RestorePreviewData?) {
        preview?.stagedDbFile?.delete()
    }

    private fun readManifestQuietly(zipFile: File): BackupManifest? {
        return try {
            ZipFile(zipFile).use { zip ->
                val entry = zip.getEntry("manifest.json") ?: return null
                val text = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).readText()
                json.decodeFromString<BackupManifest>(text)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun pruneOldBackups(dir: File, maxToKeep: Int) {
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".atbackup") }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        if (files.size <= maxToKeep) return
        files.drop(maxToKeep).forEach { it.delete() }
    }

    private fun hasValidSqliteHeader(file: File): Boolean {
        if (!file.exists() || file.length() < 16) return false
        val header = ByteArray(16)
        FileInputStream(file).use { it.read(header) }
        val expected = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        return header.contentEquals(expected)
    }

    private fun verifySqliteIntegrity(dbFile: File): Boolean {
        var sqliteDb: SQLiteDatabase? = null
        return try {
            sqliteDb = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            sqliteDb.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0).equals("ok", ignoreCase = true)
                } else {
                    false
                }
            }
        } catch (_: Exception) {
            false
        } finally {
            sqliteDb?.close()
        }
    }

    private data class TableCounts(
        val customers: Int,
        val measurements: Int,
        val orders: Int,
        val payments: Int,
        val products: Int,
        val expenses: Int
    )

    private fun readTableCounts(dbFile: File): TableCounts {
        val sqliteDb = SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
        return try {
            fun countTable(table: String): Int {
                return try {
                    sqliteDb.rawQuery("SELECT COUNT(*) FROM $table", null).use { c ->
                        if (c.moveToFirst()) c.getInt(0) else 0
                    }
                } catch (_: Exception) {
                    0
                }
            }
            TableCounts(
                customers = countTable("customers"),
                measurements = countTable("measurement_profiles"),
                orders = countTable("tailoring_orders"),
                payments = countTable("payment_transactions"),
                products = countTable("products"),
                expenses = countTable("expenses")
            )
        } finally {
            sqliteDb.close()
        }
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buf = ByteArray(16 * 1024)
            var read: Int
            while (fis.read(buf).also { read = it } != -1) {
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
