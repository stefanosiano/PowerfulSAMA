plugins {
    id(Deps.detektPlugin)
    kotlin("jvm")
}

ext {
    set("LIB_VERSION", "0.5.00") // This is the library version used when deploying the artifact
    set("ENABLE_DEPLOY", "true") // Flag whether the ci/cd workflow should deploy to sonatype or not

    set("LIB_GROUP_ID", "io.github.stefanosiano.powerful_libraries") // Maven Group ID for the artifact
    set("LIB_ARTIFACT_ID", "sama_annotations") // Maven Artifact ID for the artifact
    set("LIB_NAME", "Powerful Sama (Simplified Android Mvvm Architecture)") // Library name
    set("SITE_URL", "https://github.com/stefanosiano/PowerfulSAMA") // Homepage URL of the library
    set("GIT_URL", "https://github.com/stefanosiano/PowerfulSAMA.git") // Git repository URL
    set("LIB_DESCRIPTION", "Simplified Android Mvvm Architecture") // Library description
}

dependencies {
    implementation(Deps.kotlinStdLib)
    implementation(Deps.kotlinReflect)
    implementation(Deps.kotlinCoroutinesCore)
    implementation(Deps.kotlinPoet)
    detektPlugins(Deps.detektKtlintDependency)
    compileOnly(Deps.roomCommon)
}

apply("${rootProject.projectDir}/sonatype-publish.gradle")

detekt {
    toolVersion = Deps.detektPluginVersion
    config = files("$rootDir/config/detekt/detekt.yml")
//    allRules = true
    buildUponDefaultConfig = true
    autoCorrect = false
}
