package com.stefanosiano.powerful_libraries.sama_annotations


@Retention(AnnotationRetention.RUNTIME)
@Target(allowedTargets = [AnnotationTarget.CLASS])
/** Creates extension functions in all the project */
annotation class SamaExtensions (
    val observePowerfulSharedPreference: Boolean = true
)