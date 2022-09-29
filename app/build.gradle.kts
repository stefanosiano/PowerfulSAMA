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

        testInstrumentationRunner = Deps.androidJUnitRunner

        multiDexEnabled = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

//    lintOptions {
//        abortOnError false
//        disable "ResourceType"
//    }
}

dependencies {
    implementation(fileTree("libs") { include("*.jar") })

    implementation(Deps.kotlinStdLib)
    implementation(Deps.kotlinCoroutinesCore)
    implementation(Deps.kotlinCoroutinesAndroid)
    detektPlugins(Deps.detektKtlintDependency)

    implementation(Deps.powerfulSharedPreferences)
    implementation(Deps.powerfulSharedPreferencesLiveData)

    implementation(project(path = ":powerfulsama"))
    compileOnly(project(path = ":powerfulsama_annotations"))
    kapt(project(path = ":powerfulsama_annotations"))

    implementation(Deps.appCompat)
    implementation(Deps.androidxRecyclerView)
    implementation(Deps.androidxLifecycleExtensions)
    implementation(Deps.androidxRoomRuntime)
    implementation(Deps.androidxRoom)
    kapt(Deps.androidxRoomCompiler)
    implementation(Deps.androidxFragment)

    //TESTS
    testImplementation(Deps.androidxJunit) //'junit:junit:4.+'
    testImplementation(Deps.kotlinCoroutinesTest)
    testImplementation(Deps.kotlinTestJunit)
    testImplementation(Deps.archCoreTesting)
    testImplementation(Deps.roomTesting)
    androidTestImplementation(Deps.androidxJunit)
    androidTestImplementation(Deps.espressoCore)
    androidTestImplementation(Deps.androidxCore) // "androidx.test:core:1.5.0-alpha02")
    androidTestImplementation(Deps.androidxRunner) // "androidx.test:runner:1.5.0-alpha04")
    androidTestImplementation(Deps.androidxTestRules)
}

detekt {
    toolVersion = Deps.detektPluginVersion
    config = files("${rootDir}/config/detekt/detekt.yml")
//    allRules = true
    buildUponDefaultConfig = true
    autoCorrect = false
}
