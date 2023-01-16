// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id(Deps.detektPlugin).version(Deps.detektPluginVersion)
    id(Deps.spotlessPlugin).version(Deps.spotlessPluginVersion) apply true
}
buildscript {

    repositories {
        google()
    }
    dependencies {
        classpath(Deps.androidGradlePlugin)
        classpath(Deps.kotlinGradlePlugin)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

detekt {
    toolVersion = Deps.detektPluginVersion
    config = files("config/detekt/detekt.yml")
    buildUponDefaultConfig = true
}

spotless {
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
    isEnforceCheck = false

    kotlin {
        target("**/*.kt")
        ktlint().editorConfigOverride(mapOf("disabled_rules" to "package-name"))
    }
    kotlinGradle {
        target("**/*.kts")
        ktlint()
    }
}
