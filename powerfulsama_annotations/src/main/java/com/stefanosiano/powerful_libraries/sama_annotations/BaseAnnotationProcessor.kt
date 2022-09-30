package com.stefanosiano.powerful_libraries.sama_annotations

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

/** Base annotation processor extended by others. */
@Suppress("TooManyFunctions", "StringLiteralDuplication")
abstract class BaseAnnotationProcessor : AbstractProcessor() {

    /** Returns the annotation processor messager. */
    lateinit var messager: Messager
    /** Returns the annotation processor filer. */
    lateinit var filer: Filer

    @Synchronized
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        messager = processingEnv.messager
        filer = processingEnv.filer
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    /** Returns the path of the generated folder. */
    protected fun getGenDir() = processingEnv.options["kapt.kotlin.generated"]!!

    /** Returns the path of the module. */
    protected fun getModuleDir() = getGenDir().substringBeforeLast("/build")

    /** Log a message of kind Warning. */
    protected fun logw(message: String) {
        messager.printMessage(Diagnostic.Kind.WARNING, "$message\n")
    }

    /** Log a message of kind Note. */
    protected fun logn(message: String) {
        messager.printMessage(Diagnostic.Kind.NOTE, "$message\n")
    }

    /** Log a message of kind Error. */
    protected fun loge(message: String) {
        messager.printMessage(Diagnostic.Kind.ERROR, "$message\n")
    }

    /** Return the kotlin type of the this variable. */
    protected fun VariableElement.toKotlinType(): TypeName {
        val tn = asType().asTypeName()
        if (tn.isNullable) {
            return tn.javaToKotlinType().copy(nullable = true)
        }
        else {
            return tn.javaToKotlinType()
        }
    }


    /** Return the kotlin type of this typeMirror. */
    protected fun TypeMirror.toKotlinType() = asTypeName().javaToKotlinType()


    /** Transforms a java type to the corresponding kotlin type. */
    protected fun TypeName.javaToKotlinType(): TypeName = if (this is ParameterizedTypeName) {
        (rawType.javaToKotlinType() as ClassName).parameterizedBy(
            typeArguments.map { it.javaToKotlinType() }
        )
    } else {
        val className = JavaToKotlinClassMap.INSTANCE
            .mapJavaToKotlin(FqName(toString()))?.asSingleFqName()?.asString()
        if (className == null) {
            this
        }
        else {
            ClassName.bestGuess(className)
        }
    }

    /** Return a java type as the corresponding kotlin type. */
    protected fun getKotlinType(qualifiedName: String) = getClassName(qualifiedName).javaToKotlinType()

    /** Return the [ClassName] for a provided class [qualifiedName]. */
    protected fun getClassName(qualifiedName: String) =
        ClassName(qualifiedName.substringBeforeLast(".", ""), qualifiedName.substringAfterLast(".", ""))


    /** Return the simpleName of the targetClass of the QueryServer annotation of this element.
     * If the annotation is not present, or the targetClass is Unit, it returns the simple p of this element class. */
    fun TypeElement.getClassOrTargetClassName(): String = simpleName.toString()

    /** Return the typeMirror of the targetClass of the QueryServer annotation of this element.
     * If the annotation is not present, or the targetClass is Unit, it returns the typeMirror of this element class. */
    fun TypeElement.getClassOrTargetClass(): TypeMirror = try {
        processingEnv.elementUtils.getTypeElement(qualifiedName).asType()
    } catch (e: MirroredTypeException) {
        e.typeMirror
    }

    /**
     * Checks if the current [Element] is assignable to [qualifiedName] class.
     * In case [Element] represents a generics class, [generics] can be used to specify how many parameter there are.
     */
    fun Element.isAssignable(qualifiedName: String, generics: Int = 0): Boolean =
        isAssignable(processingEnv.elementUtils.getTypeElement(qualifiedName).asType(), generics)

    /**
     * Checks if the current [Element] is assignable to [tm] [TypeMirror].
     * In case [Element] represents a generics class, [generics] can be used to specify how many parameter there are.
     */
    @Suppress("SpreadOperator")
    fun Element.isAssignable(tm: TypeMirror, generics: Int = 0): Boolean {
        val geners = (0 until generics).map { processingEnv.typeUtils.getWildcardType(null, null) }.toTypedArray()
        val declaredType =
            processingEnv.typeUtils.getDeclaredType(processingEnv.typeUtils.asElement(tm) as? TypeElement, *geners)
        return processingEnv.typeUtils.isAssignable(this.asType(), declaredType)
    }

}

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
