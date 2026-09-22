plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // No Compose compiler plugin, deliberately. Its version has to equal the *host app's* Kotlin
    // version exactly, so declaring it here pinned every partner to ours — a host on any other
    // Kotlin failed with "This version of the Compose Compiler requires Kotlin version X but you
    // appear to be using Y". Nothing in this module is @Composable now: the SDK's screen arrives
    // as a plain View from TreeRootHost.create, and the Compose requirement lives inside the
    // prebuilt AAR where no host has to match it.
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

    // No `buildFeatures { compose = true }` either — AGP then demands the Compose compiler
    // plugin, which is the host-Kotlin pin this module exists to avoid.
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
    val treeSdkVersion = (findProperty("treeSdkVersion") as String?) ?: "0.5.1"
    implementation("com.github.DRUApp.android-tree-sdk:treesdk:$treeSdkVersion")

    // ComponentActivity + enableEdgeToEdge. Named explicitly: it used to arrive with
    // activity-compose, and the SDK's own dependencies are runtime-scope to a consumer.
    implementation("androidx.activity:activity-ktx:1.9.0")
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
