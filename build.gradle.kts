// Toolchain (Issue #155): AGP 9 has built-in Kotlin support, so the separate `org.jetbrains.kotlin.android` plugin is
// no longer applied. The Kotlin Gradle plugin version used by AGP is pinned explicitly via the buildscript classpath
// (AGP 9 otherwise uses its own bundled, older Kotlin version).
buildscript {
    repositories { google(); mavenCentral() }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
        // SCA (#155): Dependabot alerts #1-#8 concern libraries pulled in by the Android Gradle Plugin's *build* classpath
        // (signing/lint/aapt tooling), not by the APK. Raise them to the first patched versions until AGP ships them.
        constraints {
            classpath("com.google.guava:guava:33.8.0-jre") { because("Dependabot #8") }
            classpath("org.bouncycastle:bcprov-jdk18on:1.87") { because("Dependabot #6, #7") }
            classpath("org.bouncycastle:bcpkix-jdk18on:1.87") { because("Dependabot #5") }
            classpath("org.bouncycastle:bcutil-jdk18on:1.85") { because("keep all Bouncy Castle modules on one version") }
            classpath("org.bitbucket.b_c:jose4j:0.9.6") { because("Dependabot #4") }
            classpath("org.jdom:jdom2:2.0.6.1") { because("Dependabot #3") }
            classpath("org.apache.commons:commons-lang3:3.22.0") { because("Dependabot #2") }
            classpath("org.apache.httpcomponents:httpclient:4.5.14") { because("Dependabot #1") }
        }
    }
}
plugins {
    id("com.android.application") version "9.5.2" apply false
}
