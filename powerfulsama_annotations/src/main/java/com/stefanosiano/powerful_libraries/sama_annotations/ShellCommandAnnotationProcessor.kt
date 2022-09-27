package com.stefanosiano.powerful_libraries.sama_annotations

import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

/** Annotation processor that allows to run a custom shell script on build. */
class ShellCommandAnnotationProcessor : BaseAnnotationProcessor() {

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(ShellCommand::class.java.name)

    override fun process(set: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        messager.printMessage(Diagnostic.Kind.NOTE, "start running checks")

        val annotations = roundEnv.getElementsAnnotatedWith(ShellCommand::class.java)
            .filter { it.kind == ElementKind.CLASS }
            .map { it.getAnnotation(ShellCommand::class.java) }

        val moduleDir = getModuleDir()

        val commandsFromCmds: List<Pair<String, Array<String>>> =
            annotations.filterNotNull().flatMap { it.cmds.toList() }.map { Pair(it.value, it.params) }
        val commandsFromScripts: List<Pair<String, Array<String>>> =
            annotations.filterNotNull().flatMap { it.scripts.toList() }.map {
                val cmd = if(it.value.startsWith("/")) it.value else "$moduleDir/${it.value}"
                Pair(cmd, it.params)
            }

        commandsFromCmds.plus(commandsFromScripts).forEach {
            val cmd = it.first
            val params = it.second
            val result = runCommand(cmd, params)

            if(result.trim().isNotEmpty()) {
                messager.printMessage(Diagnostic.Kind.ERROR, result.trim())
                return true
            }
        }

        return false
    }


    private fun runCommand(cmd: String, params: Array<String>): String {

        val result: String = try {
            var command = cmd
            params.filter { it.trim().isNotEmpty() }
                .forEachIndexed { index, s -> command = command.replace("\$${index + 1}", s) }
            command = command.replace("then ;", "then ").replace("else ;", "else ").trim()

            messager.printMessage(Diagnostic.Kind.WARNING, "running $command")

            val proc = ProcessBuilder("sh", "-c", cmd)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            proc.waitFor(60, TimeUnit.MINUTES)
            val reader = proc.inputStream.bufferedReader()
            val res = reader.readLines()
            val readerErr = proc.errorStream.bufferedReader()
            val err = readerErr.readLines()
            proc.destroy()
            reader.close()
            readerErr.close()
            val result = res.toMutableList()
            result.addAll(err)
            result.filter { it.trim().isNotEmpty() }.joinToString(" ; ")
        } catch (e: IOException) {
            e.localizedMessage
        }
        return result
    }
}
