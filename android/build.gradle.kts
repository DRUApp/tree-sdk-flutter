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
    // android/settings.gradle.kts). A host on a different Kotlin version MUST change this to
    // match, or the build fails with "This version of the Compose Compiler requires Kotlin
    // version X but you appear to be using Y".
    //
    // 2.2.20, not the 2.2.0 the partner compatibility matrix names: Flutter 3.38.9's own Gradle
    // plugin refuses anything below 2.2.20 ("Your project's Kotlin version (2.2.0) is lower than
    // Flutter's minimum supported version of 2.2.20"), so 2.2.0 and Flutter 3.38.9 cannot coexist.
    // This is the nearest version that satisfies both.
    //
    // If a host project is still on Kotlin 1.9.x, drop this plugin entirely and use
    // composeOptions { kotlinCompilerExtensionVersion = "1.5.14" } instead (matching
    // android-tree-sdk/treesdk's own build.gradle.kts) — K2's plugin didn't exist before 2.0.
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
}

group = "com.dru.tree_sdk_flutter"

android {
    namespace = "com.dru.tree_sdk_flutter"
    // API 36, matching the partner compatibility matrix. This module is compiled inside the host
    // app's build, so its compileSdk must not exceed the host's - 36 is theirs exactly.
    compileSdk = 36

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
    // For a locally published SDK build — `./gradlew :treesdk:publishToMavenLocal` in
    // android-tree-sdk, with -PtreeSdkVersion naming it. Harmless otherwise: mavenLocal
    // has no such version, so the vendored copy below answers.
    mavenLocal()
}

// The SDK is vendored at android/maven as an AAR plus its POM — the Android
// counterpart of ios/tree_sdk_flutter/TreeSDK.xcframework — so a host resolves a
// compiled artifact with no JitPack, no token and no access to the SDK's repository.
//
// It has to be injected into every project rather than declared above, because a
// plugin's own repositories are not consulted for the dependency that matters: the
// *app* resolves the runtime classpath, against the *app's* repositories. Declaring
// it on this project alone fails with "Could not find … treesdk" on every AGP, which
// is also why the JitPack setup this replaces never worked for a host without its own
// repository line. The path is this plugin's own directory, wherever pub put it.
val treeSdkMaven = file("maven")   // captured here: inside allprojects, projectDir is each project's own
rootProject.allprojects {
    repositories {
        maven { url = treeSdkMaven.toURI() }
    }
}


dependencies {
    // The version vendored in android/maven. Bump it with the AAR, never alone:
    // android-tree-sdk/tools/build-aar.sh publishes exactly one version and clears the
    // previous, so a mismatch fails to resolve rather than building something stale.
    // Override with -PtreeSdkVersion=… plus mavenLocal() for local SDK work.
    val treeSdkVersion = (findProperty("treeSdkVersion") as String?) ?: "0.5.0"
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
