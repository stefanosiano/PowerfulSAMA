plugins {
    id(Deps.androidLibrary)
    id(Deps.detektPlugin)
    kotlin("android")
    kotlin("kapt")
}

ext {
    set("LIB_VERSION", "0.4.22") // This is the library version used when deploying the artifact
    set("ENABLE_DEPLOY", "true") //Flag whether the ci/cd workflow should deploy to sonatype or not

    set("LIB_GROUP_ID", "io.github.stefanosiano.powerful_libraries")                // Maven Group ID for the artifact
    set("LIB_ARTIFACT_ID", "sama")                                                  // Maven Artifact ID for the artifact
    set("LIB_NAME", "Powerful Sama (Simplified Android Mvvm Architecture)")         // Library name
    set("SITE_URL", "https://github.com/stefanosiano/PowerfulSAMA")                 // Homepage URL of the library
    set("GIT_URL", "https://github.com/stefanosiano/PowerfulSAMA.git")              // Git repository URL
    set("LIB_DESCRIPTION", "Simplified Android Mvvm Architecture")                  // Library description
}

android {
    compileSdk = 33

    defaultConfig {
        minSdk = 16
        targetSdk = 33
        consumerProguardFiles("sama-proguard-rules.pro")
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
//        compose = true // Enables Jetpack Compose for this module
    }
}

dependencies {
    implementation(Deps.kotlinStdLib)
    implementation(Deps.kotlinCoroutinesCore)
    implementation(Deps.kotlinCoroutinesAndroid)
//    implementation "org.jetbrains.kotlin:kotlin-reflect:$kotlin_version"

    //google libraries
    implementation(Deps.appCompat)
    implementation(Deps.androidxActivity)
    implementation(Deps.androidxRecyclerView)
    implementation(Deps.androidMaterial)

    // Architecture Components
    implementation(Deps.androidxLifecycleExtensions)                    // ViewModel and LiveData
    implementation(Deps.androidxDocumentFile)                           // DocumentFile
    implementation(Deps.androidxPagingRuntime)                          // Paging
    kapt(Deps.androidxRoomCompiler)                                     // Room annotations
    implementation(Deps.androidxRoom)                                   // Kotlin Extensions and Coroutines support for Room
}

apply("${rootProject.projectDir}/sonatype-publish.gradle")

detekt {
    toolVersion = Deps.detektPluginVersion
//    config = files("${rootDir}/config/detekt/detekt.yml")
    allRules = true
//    buildUponDefaultConfig = true
    autoCorrect = false
}
