plugins {
    id(Deps.androidLibrary)
    id(Deps.detektPlugin)
    kotlin("android")
    kotlin("kapt")
}

ext {
    set("LIB_VERSION", "0.4.22") // This is the library version used when deploying the artifact
    set("ENABLE_DEPLOY", "true") // Flag whether the ci/cd workflow should deploy to sonatype or not

    set("LIB_GROUP_ID", "io.github.stefanosiano.powerful_libraries") // Maven Group ID for the artifact
    set("LIB_ARTIFACT_ID", "sama") // Maven Artifact ID for the artifact
    set("LIB_NAME", "Powerful Sama (Simplified Android Mvvm Architecture)") // Library name
    set("SITE_URL", "https://github.com/stefanosiano/PowerfulSAMA") // Homepage URL of the library
    set("GIT_URL", "https://github.com/stefanosiano/PowerfulSAMA.git") // Git repository URL
    set("LIB_DESCRIPTION", "Simplified Android Mvvm Architecture") // Library description
}

android {
    compileSdk = Deps.sdkCompile

    defaultConfig {
        minSdk = Deps.sdkMin
        targetSdk = Deps.sdkTarget
        consumerProguardFiles("sama-proguard-rules.pro")
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
        // Enables Jetpack Compose for this module: compose = true
    }
}

dependencies {
    implementation(Deps.appCompat)
    implementation(Deps.kotlinStdLib)
    implementation(Deps.kotlinReflect)
    implementation(Deps.kotlinCoroutinesCore)
    implementation(Deps.kotlinCoroutinesAndroid)
    detektPlugins(Deps.detektKtlintDependency)

    // google libraries
    implementation(Deps.activityKtx)
    implementation(Deps.recyclerView)
    implementation(Deps.material)
    implementation(Deps.lifecycleExtensions)
    implementation(Deps.documentFile)
    implementation(Deps.pagingRuntimeKtx)
    compileOnly(Deps.roomCommon)

    testImplementation(Deps.kotlinTestJunit)
    testImplementation(Deps.robolectric)
    testImplementation(Deps.androidxCore)
    testImplementation(Deps.androidxRunner)
    testImplementation(Deps.androidxTestCoreKtx)
    testImplementation(Deps.androidxTestRules)
    testImplementation(Deps.androidxJunit)
    testImplementation(Deps.androidxCoreKtx)
    testImplementation(Deps.mockitoKotlin)
    testImplementation(Deps.mockk)
}

apply("${rootProject.projectDir}/sonatype-publish.gradle")

detekt {
    toolVersion = Deps.detektPluginVersion
    config = files("$rootDir/config/detekt/detekt.yml")
//    allRules = true
    buildUponDefaultConfig = true
    autoCorrect = false
}
