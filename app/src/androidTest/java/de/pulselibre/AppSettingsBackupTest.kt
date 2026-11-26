package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.UUID

/** Owner request 26.11.2026: personal settings travel with the backup (v12) and come back on restore. */
@RunWith(AndroidJUnit4::class)
class AppSettingsBackupTest {
    private val base = ApplicationProvider.getApplicationContext<Context>()
    private val prefix = "settings-test-${UUID.randomUUID()}-"

    /** Own files, databases and preference names, so the test never touches the app's real settings. */
    private fun isolated(root: File): Context = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
        override fun getCacheDir(): File = File(root, "cache").apply { mkdirs() }
        override fun getDatabasePath(name: String): File = File(root, name)
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = base.getSharedPreferences(prefix + name, mode)
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
            openOrCreateDatabase(name, mode, factory, null)
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?,
                                          errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
    }

    private fun cleanup() = AppSettingsBackup.STORES.forEach { base.deleteSharedPreferences(prefix + it) }

    @Test fun profileThemeLanguageAndLayoutSurviveBackupAndRestore() {
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "settings-backup-").toFile()
        val context = isolated(root)
        try {
            CirqaDatabase(context).use { it.ensureHistoricalStoreCopied() }
            context.getSharedPreferences("local_personal_profile", 0).edit()
                .putString("display_name", "Testperson").putString("birth_date", "1980-05-04")
                .putString("weight_kg", "81.5").putString("height_cm", "180").putInt("spo2_display_green_from", 94)
                .putBoolean("spo2_device_validation_recorded", true).commit()
            context.getSharedPreferences("app_appearance", 0).edit().putString("theme", "dark").putString("language", "fr").commit()
            context.getSharedPreferences("activity_detection", 0).edit().putInt("window_minutes", 7).commit()
            context.getSharedPreferences("recording_settings", 0).edit().putBoolean("auto_start", true).commit()
            context.getSharedPreferences("dashboard_display", 0).edit().putString("layout", "pulse,spo2").putLong("t", 1L shl 40)
                .putFloat("f", 1.25f).commit()
            // Technical state is not personal configuration and must not travel with the backup.
            context.getSharedPreferences(CirqaSyncAttemptGate.PREFS_NAME, 0).edit().putBoolean("attempted", true).commit()

            val backup = ByteArrayOutputStream().also { DatabaseBackup.write(context, it) }.toByteArray()

            // Change everything afterwards, including a key the backup does not have.
            context.getSharedPreferences("local_personal_profile", 0).edit().clear().putString("display_name", "Andere")
                .putString("profile_note", "neu").commit()
            context.getSharedPreferences("app_appearance", 0).edit().putString("theme", "light").remove("language").commit()
            context.getSharedPreferences("dashboard_display", 0).edit().clear().commit()

            val plan = DatabaseBackup.prepareRestore(context, ByteArrayInputStream(backup))
            assertTrue(plan.containsSettings)
            assertEquals(13L, plan.appSettings)
            plan.use { DatabaseBackup.restore(context, it) }

            val profile = context.getSharedPreferences("local_personal_profile", 0)
            assertEquals("Testperson", profile.getString("display_name", null))
            assertEquals("1980-05-04", profile.getString("birth_date", null))
            assertEquals("81.5", profile.getString("weight_kg", null))
            assertEquals(94, profile.getInt("spo2_display_green_from", 0))
            assertTrue(profile.getBoolean("spo2_device_validation_recorded", false))
            assertNull("key added after the backup is gone", profile.getString("profile_note", null))
            assertEquals("dark", context.getSharedPreferences("app_appearance", 0).getString("theme", null))
            assertEquals(AppLanguage.FRENCH, AppLanguageSettings.preference(context))
            assertEquals(7, context.getSharedPreferences("activity_detection", 0).getInt("window_minutes", 0))
            assertTrue(context.getSharedPreferences("recording_settings", 0).getBoolean("auto_start", false))
            val layout = context.getSharedPreferences("dashboard_display", 0)
            assertEquals("pulse,spo2", layout.getString("layout", null))
            assertEquals(1L shl 40, layout.getLong("t", 0))
            assertEquals(1.25f, layout.getFloat("f", 0f), 0f)
        } finally {
            cleanup(); base.deleteSharedPreferences(prefix + CirqaSyncAttemptGate.PREFS_NAME); root.deleteRecursively()
        }
    }

    @Test fun strictParserRejectsForeignStoresTypesAndOversizedValues() {
        val ok = AppSettingsBackup.parse("""{"store":"app_appearance","key":"theme","type":"string","value":"dark"}""")
        assertEquals("dark", ok.value)
        listOf(
            """{"store":"cirqa_sync_attempt","key":"x","type":"boolean","value":true}""",          // technical store
            """{"store":"app_appearance","key":"bad key!","type":"string","value":"x"}""",
            """{"store":"app_appearance","key":"theme","type":"set","value":[]}""",
            """{"store":"app_appearance","key":"theme","type":"int","value":"7"}""",
            """{"store":"app_appearance","key":"theme","type":"string","value":"${"x".repeat(16 * 1024 + 1)}"}""",
            """{"store":"app_appearance","key":"theme","type":"string","value":"x","extra":1}"""
        ).forEach { line ->
            try { AppSettingsBackup.parse(line); fail("accepted: ${line.take(80)}") } catch (_: Exception) { }
        }
        try {
            AppSettingsBackup.validateAll(sequenceOf(
                """{"store":"app_appearance","key":"theme","type":"string","value":"dark"}""",
                """{"store":"app_appearance","key":"theme","type":"string","value":"light"}"""))
            fail("duplicate accepted")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun aBackupWithoutSettingValuesRestoresDefaults() {
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "settings-backup-old-").toFile()
        val context = isolated(root)
        try {
            CirqaDatabase(context).use { it.ensureHistoricalStoreCopied() }
            val backup = ByteArrayOutputStream().also { DatabaseBackup.write(context, it) }.toByteArray()
            context.getSharedPreferences("local_personal_profile", 0).edit().putString("display_name", "Bleibt").commit()
            val plan = DatabaseBackup.prepareRestore(context, ByteArrayInputStream(backup))
            // A v12 backup without values means defaults: everything in the five stores is cleared.
            assertTrue(plan.containsSettings); assertEquals(0L, plan.appSettings)
            plan.use { DatabaseBackup.restore(context, it) }
            assertFalse(context.getSharedPreferences("local_personal_profile", 0).contains("display_name"))
        } finally { cleanup(); root.deleteRecursively() }
    }
}

/** Owner feedback 25.11.2026: every long backup step reports progress, ending at 100 %. */
@RunWith(AndroidJUnit4::class)
class BackupProgressReportTest {
    @Test fun backupCheckAndRestoreReportTheirSteps() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "progress-").toFile()
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = root
            override fun getDatabasePath(name: String): File = File(root, name)
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = base.getSharedPreferences("progress-test-$name", mode)
            override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
                openOrCreateDatabase(name, mode, factory, null)
            override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?,
                                              errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
                SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
        }
        try {
            CirqaDatabase(context).use { it.ensureHistoricalStoreCopied() }
            val seen = mutableListOf<Triple<BackupProgress.Phase, Int, Int>>()
            val sink = BackupProgress { p, d, t -> seen += Triple(p, d, t) }
            val backup = ByteArrayOutputStream().also { DatabaseBackup.write(context, it, sink) }.toByteArray()
            val writes = seen.filter { it.first == BackupProgress.Phase.WRITE }
            assertTrue(seen.first().first == BackupProgress.Phase.SNAPSHOT)
            assertEquals(writes.last().third, writes.last().second) // all entries written
            assertEquals(Triple(BackupProgress.Phase.COPY, 100, 100), seen.last())
            val percents = seen.mapNotNull { BackupProgressText.percent(it.first, it.second, it.third) }
            assertEquals(percents, percents.sorted())

            seen.clear()
            val plan = DatabaseBackup.prepareRestore(context, ByteArrayInputStream(backup), sink)
            assertEquals(listOf(BackupProgress.Phase.EXTRACT, BackupProgress.Phase.VALIDATE, BackupProgress.Phase.REFERENCES),
                seen.map { it.first }.distinct())
            seen.clear()
            plan.use { DatabaseBackup.restore(context, it, sink) }
            assertEquals(listOf(BackupProgress.Phase.RESTORE, BackupProgress.Phase.SETTINGS), seen.map { it.first }.distinct())
        } finally {
            AppSettingsBackup.STORES.forEach { base.deleteSharedPreferences("progress-test-$it") }
            root.deleteRecursively()
        }
    }
}
