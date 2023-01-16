package com.stefanosiano.powerful_libraries.sama

internal class TryCatchExtensions

/** Try to execute [toTry] in a try catch block, return null if an exception is raised. */
inline fun <T> tryOrNull(toTry: () -> T): T? = tryOr(null, toTry)

/** Try to execute [toTry] in a try catch block, return [default] if an exception is raised. */
@Suppress("TooGenericExceptionCaught")
inline fun <T> tryOr(default: T, toTry: () -> T): T =
    try {
        toTry()
    } catch (ignored: Exception) {
        default
    }

/** Execute [toTry] in a try catch block, prints the exception and returns [default] if an exception is raised. */
@Suppress("TooGenericExceptionCaught", "PrintStackTrace")
inline fun <T> tryOrPrint(default: T, toTry: () -> T): T =
    try {
        toTry()
    } catch (e: Exception) {
        e.printStackTrace()
        default
    }

/** Try to execute [toTry] in a try catch block, prints the exception and returns [null] if an exception is raised. */
inline fun <T> tryOrPrint(toTry: () -> T): T? = tryOrPrint(null, toTry)
