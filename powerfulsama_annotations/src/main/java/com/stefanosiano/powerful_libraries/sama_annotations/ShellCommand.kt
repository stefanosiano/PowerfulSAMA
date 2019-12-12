package com.stefanosiano.powerful_libraries.sama_annotations

@Retention(AnnotationRetention.SOURCE)
@Target(allowedTargets = [AnnotationTarget.CLASS])
annotation class ShellCommand (
    /** Array of scripts to call. If their names start with a '/' it is considered an absolute path to the script. Otherwise it is considered relative to the module folder */
    val scripts: Array<Script> = [],
    /** Array of shell commands to run */
    val cmds: Array<Cmd> = []
)

annotation class Script (
    /** Name of the script. If it starts with a '/' it is considered an absolute path to the script. Otherwise it is considered relative to the module folder */
    val value: String,
    /** Optional parameters of the script */
    val params: Array<String> = []
)

annotation class Cmd (
    /** Shell command to run */
    val value: String,
    /** Optional parameters of the command */
    val params: Array<String> = []
)

/*
package com.example;	// PackageElement

public class Foo {		// TypeElement

	private int a;		// VariableElement
	private Foo other; 	// VariableElement

	public Foo () {} 	// ExecuteableElement

	public void setA ( 	// ExecuteableElement
	                 int newA	// TypeElement
	                 ) {}
}
 */