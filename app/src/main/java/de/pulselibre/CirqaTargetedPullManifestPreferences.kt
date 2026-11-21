package de.pulselibre

import android.content.Context
import android.content.SharedPreferences

/** The manifest is intentionally app-private and excluded from Android backup. */
class CirqaTargetedPullManifestPreferences(context: Context) : CirqaTargetedPullManifest.Storage {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun get(key: String): String? = preferences.getString(key, null)

    override fun put(key: String, value: String): Boolean =
        preferences.edit().putString(key, value).commit()

    override fun remove(key: String): Boolean = preferences.edit().remove(key).commit()

    companion object {
        const val PREFERENCES_NAME = "cirqa_targeted_pull_manifest_v1"
    }
}
