package de.pulselibre

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import kotlin.math.sin
import kotlin.random.Random

/** LOKAL, nicht committen: füllt die Emulator-DB mit 60 Tagen synthetischer Daten. */
@RunWith(AndroidJUnit4::class)
class SyntheticSeedTest {
    @Test fun seed() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val rnd = Random(42)
        val now = Instant.now().epochSecond
        CirqaDatabase(ctx).use { db ->
            db.writableDatabase.beginTransaction()
            try {
                for (d in 60 downTo 0) {
                    val dayStart = (now / 86400 - d) * 86400
                    var steps = 0
                    for (m in 0 until 1440 step 10) {
                        val t = dayStart + m * 60L
                        if (t > now) break
                        val h = m / 60.0
                        val asleep = h < 6.5
                        val resting = (60 - d * 0.05 + rnd.nextInt(-2, 3)).toInt()
                        val bpm = if (asleep) resting + rnd.nextInt(-3, 6) else (resting + 14 + 12 * sin(h / 24 * 6.28) + rnd.nextInt(-8, 25)).toInt()
                        if (!asleep) steps += rnd.nextInt(0, 140)
                        val ts = Instant.ofEpochSecond(t).toString()
                        db.insertLine("""{"schema":1,"type":"reading","received_at_utc":"$ts","service":6,"bpm":$bpm,"resting_bpm":$resting}""")
                        if (m % 60 == 0) {
                            db.insertLine("""{"schema":1,"type":"reading","received_at_utc":"$ts","service":19,"spo2_percent":${rnd.nextInt(94, 100)}}""")
                            db.insertLine("""{"schema":1,"type":"reading","received_at_utc":"$ts","service":7,"steps":$steps,"goal":8000}""")
                            db.insertLine("""{"schema":1,"type":"reading","received_at_utc":"$ts","service":13,"stress":${rnd.nextInt(10, 70)}}""")
                        }
                    }
                }
                db.writableDatabase.setTransactionSuccessful()
            } finally { db.writableDatabase.endTransaction() }
        }
    }
}
