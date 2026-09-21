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
    // API 36 / minSdk 28, mirroring the partner compatibility matrix so this harness exercises
    // the same surface their app will.
    compileSdk = 36
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.dru.tree_sdk_flutter_example"
        // tree_sdk_flutter requires 26+; set to 28 to match the partner matrix.
        minSdk = 28
        targetSdk = 36
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

            // R8 ON. Unshrunk, the four dex files came to 55 MB of the tester APK's 109 MB -
            // every class of Compose, Moshi, OkHttp and Health Connect, none of them reachable.
            //
            // What made this safe to turn on: the Tree SDK decodes JSON with Moshi's
            // KotlinJsonAdapterFactory - pure reflection, invisible to R8 - and treesdk now ships
            // a consumer-rules.pro covering exactly that surface (wire models, @Json fields
            // anywhere, wire enums, kotlin.Metadata, Continuation). Resource shrinking is safe
            // for the same documented reason: nothing in the SDK looks a resource up by name
            // with getIdentifier(), so aapt2 can see every reference.
            //
            // Still the one change here that can fail at runtime and ONLY in release, so smoke-
            // test login + diary save + Nawaf on a release build before handing it to anyone. If
            // something does break, set both back to false, ship, and send the stack trace: the
            // fix is a line in treesdk/consumer-rules.pro, not staying unminified.
            isMinifyEnabled = true
            isShrinkResources = true
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
