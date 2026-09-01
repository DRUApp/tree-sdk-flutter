allprojects {
    repositories {
        // mavenLocal() FIRST, on purpose — this is what lets the build skip JitPack entirely.
        //
        // tree_sdk_flutter/android/build.gradle.kts depends on
        // "com.github.DRUApp.android-tree-sdk:treesdk:$treeSdkVersion" and declares
        // https://jitpack.io as a repository, which needs a token because that GitHub repo is
        // private. android-tree-sdk's own treesdk/build.gradle.kts publishes the exact same
        // coordinates (group "com.github.DRUApp.android-tree-sdk", artifact "treesdk", version
        // "main-SNAPSHOT") to mavenLocal, precisely so a local checkout can stand in for JitPack.
        // Because these repositories are declared on the root project, they are consulted before
        // the plugin's own jitpack entry — so a locally published AAR wins and JitPack is never
        // asked (and never gets the chance to 401).
        //
        // Publish it once with:
        //   cd ../../android-tree-sdk && ./gradlew :treesdk:publishToMavenLocal
        // (or run example/tools/publish_local_sdk.sh), and re-run it whenever you change the
        // native SDK.
        mavenLocal()
        google()
        mavenCentral()
    }

    // "main-SNAPSHOT" is a changing version, which Gradle would otherwise cache for 24h — that
    // would silently ignore a fresh publishToMavenLocal. Always re-resolve it instead.
    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(0, "seconds")
    }
}

val newBuildDir: Directory = rootProject.layout.buildDirectory.dir("../../build").get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
