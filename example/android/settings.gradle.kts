pluginManagement {
    val flutterSdkPath = run {
        val properties = java.util.Properties()
        file("local.properties").inputStream().use { properties.load(it) }
        val flutterSdkPath = properties.getProperty("flutter.sdk")
        require(flutterSdkPath != null) { "flutter.sdk not set in local.properties" }
        flutterSdkPath
    }

    includeBuild("$flutterSdkPath/packages/flutter_tools/gradle")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.flutter.flutter-plugin-loader") version "1.0.0"
    // These two versions are load-bearing — read before changing either.
    //
    // AGP MUST stay on 8.x. tree_sdk_flutter/android/build.gradle.kts applies
    // "org.jetbrains.kotlin.android" (and Flutter's own app module applies "kotlin-android"),
    // and AGP 9.0+ hard-fails that: "The 'org.jetbrains.kotlin.android' plugin is no longer
    // required for Kotlin support since AGP 9.0." 8.12.3 matches android-tree-sdk's committed
    // root build.gradle.kts, i.e. the newest 8.x known to build this stack.
    //
    // Kotlin MUST be 2.4.0. tree_sdk_flutter's android/build.gradle.kts applies
    // "org.jetbrains.kotlin.plugin.compose" version 2.4.0, and the Compose compiler plugin's
    // version has to equal the Kotlin version actually in use — a mismatch fails at compile
    // time. (This is the "flutter_application_1 test host" combination its comment refers to.)
    id("com.android.application") version "8.12.3" apply false
    id("org.jetbrains.kotlin.android") version "2.4.0" apply false
}

include(":app")
