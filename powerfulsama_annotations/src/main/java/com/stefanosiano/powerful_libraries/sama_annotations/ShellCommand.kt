package com.stefanosiano.powerful_libraries.sama_annotations

/** Array of scripts or commands to call on build. */
@Retention(AnnotationRetention.SOURCE)
@Target(allowedTargets = [AnnotationTarget.CLASS])
annotation class ShellCommand (
    /**
     * Array of scripts to call. If their names start with a '/' it is considered an absolute path to the script.
     * Otherwise it is considered relative to the module folder.
     */
    val scripts: Array<Script> = [],
    /** Array of shell commands to run. */
    val cmds: Array<Cmd> = []
)

/** Annotation that represents a script to call on build. */
annotation class Script (
    /**
     * Name of the script. If it starts with a '/' it is considered an absolute path to the script.
     * Otherwise it is considered relative to the module folder.
     */
    val value: String,
    /** Optional parameters of the script. */
    val params: Array<String> = []
)

/** Annotation that represents a command to call on build. */
annotation class Cmd (
    /** Shell command to run. */
    val value: String,
    /** Optional parameters of the command. */
    val params: Array<String> = []
)
