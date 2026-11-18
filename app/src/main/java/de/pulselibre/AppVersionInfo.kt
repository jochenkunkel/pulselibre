package de.pulselibre

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.core.content.pm.PackageInfoCompat

/** Version and build of the installed app, shown in the settings so installed APKs can be told apart. */
internal data class AppVersionInfo(val version: String, val build: Long, val debuggable: Boolean) {
    /** "2.0.0 · Build 996" */
    fun short(): String = "$version · Build $build"

    fun detail(): String = short() + if (debuggable) " · Debug-Signatur" else ""

    companion object {
        /** versionName looks like "2.0.0_build996"; the build number comes from versionCode. */
        fun of(versionName: String?, versionCode: Long, debuggable: Boolean) =
            AppVersionInfo((versionName ?: "?").substringBefore("_build"), versionCode, debuggable)

        fun read(context: Context): AppVersionInfo = try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            of(info.versionName, PackageInfoCompat.getLongVersionCode(info),
                context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
        } catch (_: Exception) { AppVersionInfo("unbekannt", 0, false) }
    }
}
