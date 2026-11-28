package de.pulselibre

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build

/**
 * #773: Android sets the database locale on every open. When another connection holds the database at that moment,
 * the locale change fails ("Failed to change locale ... to 'de_DE'") and the helper falls back to read-only, so
 * writes of a sync can fail. The app uses no LOCALIZED collation, so the locale step is switched off.
 */
internal object SqliteOpen {
    fun skipLocale(helper: SQLiteOpenHelper) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            helper.setOpenParams(SQLiteDatabase.OpenParams.Builder()
                .addOpenFlags(SQLiteDatabase.NO_LOCALIZED_COLLATORS)
                .build())
        }
    }
}
