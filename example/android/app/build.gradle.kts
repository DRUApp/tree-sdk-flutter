import java.io.FileInputStream
import java.util.Properties

// Optional real signing key. Create android/key.properties (gitignored) with:
//   storeFile=/absolute/path/to/upload-keystore.jks
//   storePassword=...
//   keyAlias=...
//   keyPassword=...
// Generate one with:
//   keytool -genkey -v -keystore ~/upload-keystore.jks -keyalg RSA \
//     -keysize 2048 -validity 10000 -alias upload
// Without that file the release build falls back to the debug key, which still installs
// fine for internal distribution (BetaDrop etc.) — but every machine has a *different*
// debug key, so builds from elsewhere can't update an install from here.
val keystoreProperties = Properties().apply {
    val f = rootProject.file("key.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}

plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after Android/Kotlin.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.dru.tree_sdk_flutter_example"
    compileSdk = 34
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.dru.tree_sdk_flutter_example"
        // tree_sdk_flutter's README requires minSdkVersion 26+.
        minSdk = 26
        targetSdk = 34
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    signingConfigs {
        if (keystoreProperties.getProperty("storeFile") != null) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")

            // R8 OFF on purpose. Flutter shrinks release builds by default, and the Tree
            // SDK decodes JSON with Moshi's KotlinJsonAdapterFactory — pure reflection.
            // treesdk's consumer-rules.pro keeps only `modules.**.data.**` plus a
            // hand-added TokenBundle, while Moshi-shaped types also live in
            // com.dru.treesdk.session/config. Anything the rules miss gets stripped and
            // fails at runtime *only in release* — the worst possible thing to hand a
            // tester. Re-enable once the SDK's keep rules are known-complete.
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
}

// `android { kotlinOptions { jvmTarget = "17" } }` is a compile-error-level deprecation under
// Kotlin 2.x — this is the replacement, matching tree_sdk_flutter/android/build.gradle.kts.
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}
