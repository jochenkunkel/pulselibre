package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseLockedException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Isolated stress probe for SQLiteOpenHelper opens during another helper's write transaction. */
@RunWith(AndroidJUnit4::class)
class CirqaSqliteOpenContentionTest {
    @Test fun boundedBusyRetryRecoversFreshHelperOpensAfterWriterReleasesLock() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "sqlite-open-contention-").toFile()
        val context = isolatedContext(base, root)
        val workers = 4
        val ready = CountDownLatch(workers)
        val start = CountDownLatch(1)
        val firstOpenOutcomePerWorker = CountDownLatch(workers)
        val busyFailures = AtomicInteger()
        val failures = ConcurrentLinkedQueue<String>()
        val executor = Executors.newFixedThreadPool(workers)
        val owner = CirqaDatabase(context)
        var ownerDatabase: SQLiteDatabase? = null
        try {
            CirqaDatabase(context).use { seed ->
                seed.writableDatabase.execSQL(
                    "CREATE TABLE IF NOT EXISTS pl_open_probe (value INTEGER NOT NULL)"
                )
            }
            val database = owner.writableDatabase
            ownerDatabase = database
            database.beginTransaction()
            database.execSQL("INSERT INTO pl_open_probe(value) VALUES (1)")

            val opens = (0 until workers).map {
                executor.submit {
                    ready.countDown()
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        failures.add("start-barrier-timeout")
                        return@submit
                    }
                    val observedFirstOutcome = AtomicBoolean(false)
                    try {
                        CirqaSqliteBusyRetry.runOnce(
                            retryable = { exception ->
                                if (observedFirstOutcome.compareAndSet(false, true)) {
                                    firstOpenOutcomePerWorker.countDown()
                                }
                                if (exception is SQLiteDatabaseLockedException) {
                                    busyFailures.incrementAndGet()
                                    true
                                } else false
                            }
                        ) {
                            val result = CirqaDatabase(context).use { helper ->
                                helper.readableDatabase.rawQuery(
                                    "SELECT COUNT(*) FROM pl_open_probe", null
                                ).use { rows -> check(rows.moveToFirst()) }
                            }
                            if (observedFirstOutcome.compareAndSet(false, true)) {
                                firstOpenOutcomePerWorker.countDown()
                            }
                            result
                        }
                    } catch (failure: Exception) {
                        failures.add(failure.javaClass.simpleName)
                    }
                }
            }
            assertTrue("workers did not reach the start barrier", ready.await(5, TimeUnit.SECONDS))
            start.countDown()
            assertTrue(
                "open attempts did not reach a success/error outcome; ${failures.joinToString()}",
                firstOpenOutcomePerWorker.await(12, TimeUnit.SECONDS)
            )
            ownerDatabase?.endTransaction()
            opens.forEach { it.get(15, TimeUnit.SECONDS) }
            assertTrue("busy retry was not exercised: ${busyFailures.get()}", busyFailures.get() > 0)
            assertTrue("SQLite helper open failures: ${failures.joinToString()}", failures.isEmpty())
        } finally {
            start.countDown()
            ownerDatabase?.let { db ->
                if (db.inTransaction()) db.endTransaction()
            }
            owner.close()
            executor.shutdownNow()
            executor.awaitTermination(5, TimeUnit.SECONDS)
            root.deleteRecursively()
        }
    }

    private fun isolatedContext(base: Context, root: File): Context = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
        override fun getDatabasePath(name: String): File = File(root, name)
        override fun openOrCreateDatabase(name: String, mode: Int,
            factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
            openOrCreateDatabase(name, mode, factory, null)
        override fun openOrCreateDatabase(name: String, mode: Int,
            factory: SQLiteDatabase.CursorFactory?, errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
    }
}
