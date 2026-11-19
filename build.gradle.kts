// Toolchain (Issue #155): AGP 9 has built-in Kotlin support, so the separate `org.jetbrains.kotlin.android` plugin is
// no longer applied. The Kotlin Gradle plugin version used by AGP is pinned explicitly via the buildscript classpath
// (AGP 9 otherwise uses its own bundled, older Kotlin version).
buildscript {
    repositories { google(); mavenCentral() }
    dependencies { classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21") }
}
plugins {
    id("com.android.application") version "9.5.1" apply false
}
