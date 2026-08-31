plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // Kotlin 2.0+'s Compose compiler is a separate Gradle plugin instead of the old
    // composeOptions.kotlinCompilerExtensionVersion mechanism (which K2 ignores).
    //
    // Unlike "com.android.application"/"org.jetbrains.kotlin.android" above, this one needs an
    // explicit version: those two are versionless because the *host* Flutter app's
    // android/settings.gradle.kts already declares a version for them (Gradle resolves an
    // unversioned plugin ID from whatever else in the build pins one) — but no Flutter host
    // declares "org.jetbrains.kotlin.plugin.compose" itself, so nothing provides a version for
    // it. It MUST equal the Kotlin version actually in use for this build — mismatched versions
    // fail at compile time, not silently — so keep this in lockstep with whatever
    // "org.jetbrains.kotlin.android" resolves to for the app you're testing against (check its
    // android/settings.gradle.kts). Currently matches the flutter_application_1 test host (Kotlin
    // 2.4.0). If a host project is still on Kotlin 1.9.x, drop this plugin entirely and use
    // composeOptions { kotlinCompilerExtensionVersion = "1.5.14" } instead (matching
    // android-tree-sdk/treesdk's own build.gradle.kts) — K2's plugin didn't exist before 2.0.
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"
}

group = "com.dru.tree_sdk_flutter"

android {
    namespace = "com.dru.tree_sdk_flutter"
    compileSdk = 34

    defaultConfig {
        // Matches android-tree-sdk's treesdk module. Hosts need minSdkVersion >= 26 in their own
        // android/app/build.gradle — the Android counterpart to the iOS side's
        // IPHONEOS_DEPLOYMENT_TARGET = 16.0 requirement.
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// `android { kotlinOptions { jvmTarget = "17" } }` is a hard (compile-error-level) deprecation
// under this Kotlin/AGP toolchain — not just a warning. This is the replacement, matching what
// flutter_application_1/android/app/build.gradle.kts (the Flutter template itself) already uses.
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

repositories {
    maven {
        // android-tree-sdk (github.com/DRUApp/android-tree-sdk) has no publishing step of its
        // own — JitPack builds an artifact straight from the GitHub repo, the closest Android
        // equivalent to how ../ios/tree_sdk_flutter/Package.swift resolves TreeSDK from a git tag
        // via Swift Package Manager. It's a multi-module repo, so the coordinate is JitPack's
        // "<groupId>.<repoName>:<moduleName>" form — see the dependency below.
        url = uri("https://jitpack.io")
        // The repo is private, so JitPack needs a token with access to it — the Android
        // counterpart to "the host's machine and CI need credentials for each" in this package's
        // README. Set it in the consuming project's ~/.gradle/gradle.properties as
        // `jitpackToken=...`, or export JITPACK_TOKEN. Never commit it.
        val token = (findProperty("jitpackToken") as String?) ?: System.getenv("JITPACK_TOKEN")
        if (!token.isNullOrBlank()) {
            credentials { username = token }
        }
    }
}

dependencies {
    // Pin this to a real android-tree-sdk tag (or commit SHA) once one is pushed — see this
    // package's README for how to override treeSdkVersion. "main-SNAPSHOT" tracks the tip of
    // main, which JitPack rebuilds on demand but is not reproducible across builds.
    val treeSdkVersion = (findProperty("treeSdkVersion") as String?) ?: "main-SNAPSHOT"
    implementation("com.github.DRUApp.android-tree-sdk:treesdk:$treeSdkVersion")

    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Classic View-system Material Components library — NOT the same artifact as
    // androidx.compose.material3:material3 above. Compose's material3 is a Kotlin/Compose
    // runtime library with no XML resources at all, so it cannot satisfy an XML style parent.
    // TreeSdkFlutterActivity's manifest theme (styles.xml -> TreeSdkFlutterTheme, parent
    // "Theme.Material3.Light.NoActionBar") is a classic Android View-system theme resource —
    // every Activity needs one of these even when its content is 100% Compose, because the
    // OS applies the manifest/style theme before any Compose code runs (window background,
    // status/nav bar chrome, default widget styling for things like the text-selection
    // handles and system dialogs). Only this library actually ships that style in XML.
    implementation("com.google.android.material:material:1.12.0")
}
