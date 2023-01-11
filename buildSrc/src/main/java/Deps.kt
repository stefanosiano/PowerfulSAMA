object Deps {

    // Sdk versions
    const val sdkCompile = 33
    const val sdkTarget = sdkCompile
    const val sdkMin = 14

    private const val agpVersion = "7.3.0"
    private const val kotlinVersion = "1.8.0"
    private const val coroutinesVersion = "1.5.1"
    private const val espressoVersion = "3.5.0"
    private const val roomVersion = "2.4.3"
    const val detektPluginVersion = "1.19.0"

    // Gradle plugins
    const val androidGradlePlugin = "com.android.tools.build:gradle:$agpVersion"
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
    const val createNonRsPlugin = "com.stefanosiano.powerful_libraries.imageview.plugins_non_rs"
    const val detektPlugin = "io.gitlab.arturbosch.detekt"
    const val kotlinKaptPlugin = "kotlin-kapt"
    const val androidLibrary = "com.android.library"
    const val androidApplication = "com.android.application"

    // Kotlin
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
    const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion"
    const val kotlinCoroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion"
    const val kotlinCoroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion"

    // Android
    const val appCompat = "androidx.appcompat:appcompat:1.5.1"
    const val fragmentKtx = "androidx.fragment:fragment-ktx:1.5.5"
    const val roomRuntime = "androidx.room:room-runtime:$roomVersion"
    const val roomCommon = "androidx.room:room-common:$roomVersion"
    const val roomCompiler = "androidx.room:room-compiler:$roomVersion"
    const val roomKtx = "androidx.room:room-ktx:$roomVersion"
    const val activityKtx = "androidx.activity:activity-ktx:1.6.1"
    const val recyclerView = "androidx.recyclerview:recyclerview:1.2.1"
    const val material = "com.google.android.material:material:1.7.0"
    const val lifecycleExtensions = "androidx.lifecycle:lifecycle-extensions:2.2.0"
    const val documentFile = "androidx.documentfile:documentfile:1.0.1"
    const val pagingRuntimeKtx = "androidx.paging:paging-runtime-ktx:3.1.1"

    // Tests
    const val androidJUnitRunner = "androidx.test.runner.AndroidJUnitRunner"
    const val androidxTestOrchestrator = "androidx.test:orchestrator:1.4.2"
    const val espressoCore = "androidx.test.espresso:espresso-core:$espressoVersion"
    const val espressoIdlingResource = "androidx.test.espresso:espresso-idling-resource:$espressoVersion"
    const val roomTesting = "androidx.room:room-testing:$roomVersion"
    const val archTesting = "android.arch.core:core-testing:1.1.1"

    // Other libraries
    const val detektKtlintDependency = "io.gitlab.arturbosch.detekt:detekt-formatting:$detektPluginVersion"
    const val kotlinPoet = "com.squareup:kotlinpoet:1.12.0"
    const val powerfulSharedPreferences = "io.github.stefanosiano.powerful_libraries:sharedpreferences:1.0.20"
    const val powerfulSharedPreferencesLiveData = "io.github.stefanosiano.powerful_libraries:sharedpreferences_livedata:1.0.7"

    // Test libraries
    private const val androidxTestVersion = "1.5.0"
    const val kotlinCoroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion"
    const val kotlinTestJunit = "org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion"
    const val robolectric = "org.robolectric:robolectric:4.7.3"
    const val androidxCore = "androidx.test:core:$androidxTestVersion"
    const val androidxRunner = "androidx.test:runner:$androidxTestVersion"
    const val androidxTestCoreKtx = "androidx.test:core-ktx:$androidxTestVersion"
    const val androidxTestRules = "androidx.test:rules:$androidxTestVersion"
    const val androidxJunit = "androidx.test.ext:junit:1.1.3"
    const val androidxCoreKtx = "androidx.core:core-ktx:1.7.0"
    const val mockitoKotlin = "com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0"
    const val mockk = "io.mockk:mockk:1.12.5"
}