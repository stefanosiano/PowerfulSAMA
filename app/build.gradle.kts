plugins {
    id(Deps.androidApplication)
//    id(Deps.detektPlugin)
    kotlin("android")
    kotlin("kapt")
}

android {
    compileSdk = 32

    defaultConfig {
        applicationId = "com.stefanosiano.powerfullibraries.sama_sample"
        minSdk = 16
        targetSdk = 32
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
//    detektPlugins(Deps.detektKtlintDependency)

//    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    implementation("io.github.stefanosiano.powerful_libraries:sharedpreferences:1.0.19")                 //PowerfulSharedPreferences
    implementation("io.github.stefanosiano.powerful_libraries:sharedpreferences_livedata:1.0.6")           //PowerfulSharedPreferences

    implementation(project(path = ":powerfulsama"))
    compileOnly(project(path = ":powerfulsama_annotations"))
    kapt(project(path = ":powerfulsama_annotations"))

    implementation(Deps.appCompat)
    implementation(Deps.androidxRecyclerView)
    implementation(Deps.androidxLifecycleExtensions)
    implementation(Deps.androidxRoomRuntime)
    implementation(Deps.androidxRoom)
    kapt(Deps.androidxRoomCompiler)
    implementation("androidx.fragment:fragment-ktx:1.3.5")

    //TESTS
    testImplementation(Deps.androidxJunit) //'junit:junit:4.+'
    testImplementation(Deps.kotlinCoroutinesTest)
    testImplementation("android.arch.core:core-testing:1.1.1")
    testImplementation("androidx.room:room-testing:2.3.0")                                        //Test helpers for Room
    /*
    androidTestImplementation 'androidx.test.ext:junit:1.1.3'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.4.0'
    // Core library
    androidTestImplementation 'androidx.test:core:1.4.0'
    androidTestImplementation 'androidx.test:runner:1.4.0'
    androidTestImplementation 'androidx.test:rules:1.4.0'

    // AndroidJUnitRunner and JUnit Rules
    androidTestImplementation 'androidx.test:runner:1.4.0'
    androidTestImplementation 'androidx.test:rules:1.4.0'
    */
}
/*
detekt {
    toolVersion = Deps.detektPluginVersion
    config = files("${rootDir}/config/detekt/detekt.yml")
//    allRules = true
    buildUponDefaultConfig = true
    autoCorrect = false
}*/
