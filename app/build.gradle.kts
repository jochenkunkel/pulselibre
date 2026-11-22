import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    jacoco
}
android {
    namespace = "de.pulselibre"
    compileSdk = 37
    defaultConfig {
        applicationId = "de.pulselibre"
        minSdk = 26
        targetSdk = 35
        versionCode = (providers.gradleProperty("pulselibreVersionCode").orNull?.toIntOrNull() ?: 2)
        versionName = "2.0.0_build${providers.gradleProperty("pulselibreVersionCode").orNull ?: "2"}"
        testInstrumentationRunner = "de.pulselibre.GermanTestRunner"
    }
    // CI uses one persistent private signing key supplied by GitHub Actions secrets.
    // Without these variables, assembleDebug remains an ordinary non-updatable CI debug APK.
    val ciStore = System.getenv("PULSELIBRE_SIGNING_STORE")
    val ciStorePassword = System.getenv("PULSELIBRE_SIGNING_STORE_PASSWORD")
    val ciKeyAlias = System.getenv("PULSELIBRE_SIGNING_KEY_ALIAS")
    val ciKeyPassword = System.getenv("PULSELIBRE_SIGNING_KEY_PASSWORD")
    if (!ciStore.isNullOrBlank() && !ciStorePassword.isNullOrBlank() &&
        !ciKeyAlias.isNullOrBlank() && !ciKeyPassword.isNullOrBlank()) {
        signingConfigs {
            create("stableCi") {
                storeFile = file(ciStore)
                storePassword = ciStorePassword
                keyAlias = ciKeyAlias
                keyPassword = ciKeyPassword
            }
        }
        buildTypes.getByName("debug").signingConfig = signingConfigs.getByName("stableCi")
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("stableCi")
    }
    buildTypes {
        getByName("debug") { enableUnitTestCoverage = true; enableAndroidTestCoverage = true }
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt")
            )
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.appcompat:appcompat:1.8.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}

// SCA (#155): the lint tool configuration (`androidLintTool`) resolves its own copy of build-tool libraries that the
// buildscript constraints in the root build file do not reach. None of these libraries is part of the APK; raise them
// to the first patched versions (Dependabot alerts #1, #2, #5-#8) until the Android Gradle Plugin ships them itself.
val patchedBuildToolLibraries = mapOf(
    "com.google.guava:guava" to "33.7.2-jre",
    "org.bouncycastle:bcprov-jdk18on" to "1.85",
    "org.bouncycastle:bcpkix-jdk18on" to "1.85",
    "org.bouncycastle:bcutil-jdk18on" to "1.85",
    "org.apache.commons:commons-lang3" to "3.18.0",
    "org.apache.httpcomponents:httpclient" to "4.5.14",
)
configurations.configureEach {
    resolutionStrategy.eachDependency {
        patchedBuildToolLibraries["${requested.group}:${requested.name}"]?.let { useVersion(it); because("SCA #155") }
    }
}

// #6: one report over JVM and instrumentation coverage recorded in the same CI job.
// Both execution data files must come from this checkout's compiled debug classes;
// mixing classes from different builds produces a meaningless 0 % report.
tasks.register<JacocoReport>("createCombinedCoverageReport") {
    group = "verification"
    description = "Combines JVM unit-test and emulator instrumentation coverage from this checkout."
    // Gradle must know that the class directory is produced in this build.
    dependsOn("compileDebugKotlin")
    mustRunAfter("connectedDebugAndroidTest", "testDebugUnitTest")
    val unitExec = layout.buildDirectory.file(
        "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
    )
    val androidExec = fileTree(
        layout.buildDirectory.dir("outputs/code_coverage/debugAndroidTest/connected")
    ) { include("**/coverage.ec") }
    executionData.setFrom(unitExec, androidExec)
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    classDirectories.setFrom(
        files(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"))
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    doFirst {
        val present = executionData.files.filter { it.isFile }
        check(present.any { it.name == "testDebugUnitTest.exec" }) {
            "JVM coverage data fehlt: ${unitExec.get().asFile}"
        }
        check(present.any { it.name == "coverage.ec" }) {
            "Emulator-Coverage fehlt: bitte zuerst :app:connectedDebugAndroidTest ausführen"
        }
        check(file(classDirectories.singleFile).isDirectory) {
            "Debug-Klassen fehlen; Klassen müssen im selben Job kompiliert worden sein"
        }
    }
}
