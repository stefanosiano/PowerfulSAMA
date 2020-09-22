package com.stefanosiano.powerful_libraries.sama_annotations


@Retention(AnnotationRetention.RUNTIME)
@Target(allowedTargets = [AnnotationTarget.CLASS])
/** Creates extension functions in all the project */
annotation class SamaExtensions (
    val observePowerfulSharedPreference: Boolean = true
)


@Retention(AnnotationRetention.RUNTIME)
@Target(allowedTargets = [AnnotationTarget.FIELD])
/** Ignores a field when generating default methods. By default it already ignores room's Ignore annotation */
annotation class IgnoreField