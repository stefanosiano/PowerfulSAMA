package com.stefanosiano.powerful_libraries.sama_annotations


@Retention(AnnotationRetention.RUNTIME)
@Target(allowedTargets = [AnnotationTarget.FUNCTION])
/** Creates extension functions to activities with startActivity and startActivityForResult using the annotated method to get the intent.
 * Use it in Companion Objects and require a [Context] or [Activity] as first parameter.
 * You can also specify a name to be used as the extension function name */
annotation class ActivityIntent (
    /** optional name to show as method. If not specified, the class name is used
     * e.g. name = 'MyActivity' -> startMyActivity, startMyActivityForResult */
    val name: String = ""
)