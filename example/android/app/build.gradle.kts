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

    buildTypes {
        release {
            // Signed with the debug key so `flutter build apk --release` produces an
            // installable APK immediately. Swap in your own signing config before
            // distributing this beyond your own testing.
            signingConfig = signingConfigs.getByName("debug")
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
