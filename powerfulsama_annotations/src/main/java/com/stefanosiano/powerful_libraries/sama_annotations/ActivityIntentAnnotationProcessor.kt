package com.stefanosiano.powerful_libraries.sama_annotations

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

class ActivityIntentAnnotationProcessor : AbstractProcessor() {

    lateinit var messager: Messager
    lateinit var filer: Filer

    @Synchronized
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        messager = processingEnv.messager
        filer = processingEnv.filer
    }

    override fun process(set: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        messager.printMessage(Diagnostic.Kind.NOTE, "start running checks")

        val methods = roundEnv.getElementsAnnotatedWith(ActivityIntent::class.java)
            .filter { it.kind == ElementKind.METHOD }
        if(methods.isEmpty()) return false

        val genDir = processingEnv.options["kapt.kotlin.generated"]!!
        val moduleDir = genDir.substringBeforeLast("/build")



        val generatedPackage = "com.stefanosiano.powerful_libraries.sama.generated"
        val generatedSimpleName = "ActivityIntentsExtensions"


        //checks
        methods.forEach { method ->

            if(method.enclosingElement.simpleName.toString() != "Companion") {
                messager.printMessage(Diagnostic.Kind.ERROR, "This annotation must be used for methods in Companion Objects only! Check ${method.enclosingElement.simpleName}.${method.simpleName}")
                return false
            }

            method as ExecutableElement

            val methodFullName = "${method.enclosingElement.enclosingElement.simpleName}.${method.simpleName}(${method.parameters.joinToString { it.asType().toString() }})"

            if(method.returnType.toKotlinType() != getKotlinType("android.content.Intent")) {
                messager.printMessage(Diagnostic.Kind.ERROR, "$methodFullName must return an Intent")
                return false
            }

            if(method.parameters[0].asType().toKotlinType() != getKotlinType("android.content.Context")
                && method.parameters[0].asType().toKotlinType() != getKotlinType("android.app.Activity")) {
                messager.printMessage(Diagnostic.Kind.ERROR, "$methodFullName must have a Context or an Activity as the first parameter")
                return false
            }
        }

        val functions = ArrayList<FunSpec>()

        methods.forEach { method ->
            val activityIntent = method.getAnnotation(ActivityIntent::class.java)
            method as ExecutableElement

            val clazz = method.enclosingElement.enclosingElement
            val params = method.parameters
            val toPutParams = params.filterIndexed { index, _ -> index != 0 }.joinToString { it.simpleName }.let { if (it.trim().isEmpty()) "" else ", ${it.trim()}" }

            val funName = "start${activityIntent.name.trim().ifEmpty { clazz.simpleName } }"
            val funForResultName = "start${activityIntent.name.trim().ifEmpty { clazz.simpleName } }ForResult"

            val function = FunSpec.builder(funName).receiver(getKotlinType("android.app.Activity"))
            val functionForResult = FunSpec.builder(funForResultName).receiver(getKotlinType("android.app.Activity"))
            val comment = processingEnv.elementUtils.getDocComment(method)?.trim()?.ifEmpty { null }
            comment?.let { function.addKdoc(it); functionForResult.addKdoc(it) }

            method.parameters.filterIndexed { index, _ -> index != 0 }.forEach {
                function.addParameter(ParameterSpec.builder(it.simpleName.toString(), it.toKotlinType()).build())
                functionForResult.addParameter(ParameterSpec.builder(it.simpleName.toString(), it.toKotlinType()).build())
            }

            functionForResult.addParameter(ParameterSpec.builder("requestCode", Int::class).build())
            functionForResult.addParameter(ParameterSpec.builder("options", getKotlinType("android.os.Bundle").copy(true)).defaultValue("null").build())

            function.addStatement("startActivity(%T.getIntent(this$toPutParams))", clazz)
            functionForResult.addStatement("startActivityForResult(%T.getIntent(this$toPutParams), requestCode, options)", clazz)

            functions.add(function.build())
            functions.add(functionForResult.build())
        }

        val file = FileSpec.builder(generatedPackage, generatedSimpleName)
        functions.forEach { f -> file.addFunction(f) }


        file.build().writeTo(File(genDir))

        return false
    }

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(ActivityIntent::class.java.name)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()



    /** Return the kotlin type of the this variable */
    fun VariableElement.toKotlinType(): TypeName {
        val tn = asType().asTypeName()
        if(tn.isNullable) return tn.javaToKotlinType().copy(nullable = true)
        else return tn.javaToKotlinType()
    }


    /** Return the kotlin type of this typeMirror */
    fun TypeMirror.toKotlinType() = asTypeName().javaToKotlinType()


    /** Transforms a java type to the corresponding kotlin type */
    private fun TypeName.javaToKotlinType(): TypeName = if (this is ParameterizedTypeName) {
        (rawType.javaToKotlinType() as ClassName).parameterizedBy(
            *typeArguments.map { it.javaToKotlinType() }.toTypedArray()
        )
    } else {
        val className = JavaToKotlinClassMap.INSTANCE
            .mapJavaToKotlin(FqName(toString()))?.asSingleFqName()?.asString()
        if (className == null) this
        else ClassName.bestGuess(className)
    }

    private fun getKotlinType(qualifiedName: String) =
        ClassName(qualifiedName.substringBeforeLast(".", ""), qualifiedName.substringAfterLast(".", "")).javaToKotlinType()
}