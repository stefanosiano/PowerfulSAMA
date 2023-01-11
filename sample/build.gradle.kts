plugins {
    id(Deps.androidApplication)
    id(Deps.detektPlugin)
    kotlin("android")
    kotlin("kapt")
}

android {
    compileSdk = Deps.sdkCompile

    defaultConfig {
        applicationId = "com.stefanosiano.powerfullibraries.sama_sample"
        minSdk = Deps.sdkMin
        targetSdk = Deps.sdkTarget
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

        testInstrumentationRunner = Deps.androidJUnitRunner

        // The following argument makes the Android Test Orchestrator run its
        // "pm clear" command after each test invocation. This command ensures
        // that the app's state is completely cleared between tests.
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
        // Enables Jetpack Compose for this module: compose = true
    }

    buildTypes {
        debug {
            isShrinkResources = false
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(path = ":powerfulsama"))
    compileOnly(project(path = ":powerfulsama_annotations"))
    kapt(project(path = ":powerfulsama_annotations"))

    implementation(Deps.kotlinStdLib)
    implementation(Deps.kotlinReflect)
    implementation(Deps.kotlinCoroutinesCore)
    implementation(Deps.kotlinCoroutinesAndroid)
    implementation(Deps.powerfulSharedPreferences)
    implementation(Deps.powerfulSharedPreferencesLiveData)
    implementation(Deps.appCompat)
    implementation(Deps.recyclerView)
    implementation(Deps.fragmentKtx)
    implementation(Deps.lifecycleExtensions)
    implementation(Deps.roomRuntime)
    kapt(Deps.roomCompiler)
    implementation(Deps.roomKtx)
    detektPlugins(Deps.detektKtlintDependency)

    //TESTS
    testImplementation(Deps.kotlinTestJunit)
    testImplementation(Deps.kotlinCoroutinesTest)
    testImplementation(Deps.archTesting)
    testImplementation(Deps.roomTesting)
    androidTestImplementation(Deps.androidxJunit)
    androidTestImplementation(Deps.espressoCore)

    // Core library
    androidTestImplementation(Deps.androidxCore)
    androidTestImplementation(Deps.androidxRunner)
    androidTestImplementation(Deps.androidxTestCoreKtx)
    androidTestImplementation(Deps.androidxTestRules)
}

detekt {
    toolVersion = Deps.detektPluginVersion
    config = files("${rootDir}/config/detekt/detekt.yml")
//    allRules = true
    buildUponDefaultConfig = true
    autoCorrect = false
}
