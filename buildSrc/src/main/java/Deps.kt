object Deps {

    // Sdk versions
    const val sdkCompile = 33
    const val sdkTarget = sdkCompile
    const val sdkMin = 16

    private const val agpVersion = "7.3.0"
    private const val kotlinVersion = "1.7.10"
    private const val coroutinesVersion = "1.5.1"
    private const val espressoVersion = "3.4.0"
    const val detektPluginVersion = "1.19.0"

    // Gradle plugins
    const val androidGradlePlugin = "com.android.tools.build:gradle:$agpVersion"
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
    const val detektPlugin = "io.gitlab.arturbosch.detekt"
    const val androidLibrary = "com.android.library"
    const val androidApplication = "com.android.application"

    // Kotlin
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
    const val kotlinCoroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion"
    const val kotlinCoroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion"
    const val kotlinCoroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion"

    // Android
    const val appCompat = "androidx.appcompat:appcompat:1.3.0"
    const val androidxActivity = "androidx.activity:activity-ktx:1.3.0"
    const val androidxFragment = "androidx.fragment:fragment-ktx:1.5.2"
    const val androidxRecyclerView = "androidx.recyclerview:recyclerview:1.2.1"
    const val androidMaterial = "com.google.android.material:material:1.4.0"
    const val androidxCoreKtx = "androidx.core:core-ktx:1.7.0"

    // Architecture Components
    const val androidxLifecycleExtensions = "androidx.lifecycle:lifecycle-extensions:2.2.0"     // ViewModel and LiveData
    const val androidxDocumentFile = "androidx.documentfile:documentfile:1.0.1"                 // DocumentFile
    const val androidxPagingRuntime = "androidx.paging:paging-runtime-ktx:3.0.0"                // Paging
    const val androidxRoomCompilerOld = "androidx.room:room-compiler:2.2.6"                        // Room annotations
    const val androidxRoomCompiler = "androidx.room:room-compiler:2.3.0"                        // Room annotations
    const val androidxRoom = "androidx.room:room-ktx:2.3.0"                                     // Kotlin Extensions and Coroutines support for Room
    const val androidxRoomRuntime = "androidx.room:room-runtime:2.3.0"                                     // Kotlin Extensions and Coroutines support for Room

    // Other libraries
    const val detektKtlintDependency = "io.gitlab.arturbosch.detekt:detekt-formatting:$detektPluginVersion"
    const val kotlinPoet = "com.squareup:kotlinpoet:1.12.0"
    const val powerfulSharedPreferences = "io.github.stefanosiano.powerful_libraries:sharedpreferences:1.0.20"
    const val powerfulSharedPreferencesLiveData = "io.github.stefanosiano.powerful_libraries:sharedpreferences_livedata:1.0.7"

    // Tests
    private const val androidxTestVersion = "1.4.0"

    const val androidJUnitRunner = "androidx.test.runner.AndroidJUnitRunner"
    const val androidxTestOrchestrator = "androidx.test:orchestrator:1.4.1"

    const val androidxCore = "androidx.test:core:$androidxTestVersion"
    const val androidxRunner = "androidx.test:runner:$androidxTestVersion"
    const val androidxTestCoreKtx = "androidx.test:core-ktx:$androidxTestVersion"
    const val androidxTestRules = "androidx.test:rules:$androidxTestVersion"

    const val espressoCore = "androidx.test.espresso:espresso-core:$espressoVersion"
    const val espressoIdlingResource = "androidx.test.espresso:espresso-idling-resource:$espressoVersion"

    const val archCoreTesting = "android.arch.core:core-testing:1.1.1"
    const val roomTesting = "androidx.room:room-testing:2.4.3"
    const val kotlinTestJunit = "org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion"
    const val robolectric = "org.robolectric:robolectric:4.7.3"
    const val androidxJunit = "androidx.test.ext:junit:1.1.3"
    const val mockitoKotlin = "com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0"
    const val mockk = "io.mockk:mockk:1.12.5"
}