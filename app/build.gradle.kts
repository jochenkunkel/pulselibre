plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "de.pulselibre"
    compileSdk = 35
    defaultConfig {
        applicationId = "de.pulselibre"
        minSdk = 26
        targetSdk = 35
        versionCode = (providers.gradleProperty("pulselibreVersionCode").orNull?.toIntOrNull() ?: 2)
        versionName = "2.0.0_build${providers.gradleProperty("pulselibreVersionCode").orNull ?: "2"}"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            create("stableCiDebug") {
                storeFile = file(ciStore)
                storePassword = ciStorePassword
                keyAlias = ciKeyAlias
                keyPassword = ciKeyPassword
            }
        }
        buildTypes.getByName("debug").signingConfig = signingConfigs.getByName("stableCiDebug")
    }
    buildTypes { getByName("debug") { enableUnitTestCoverage = true } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
