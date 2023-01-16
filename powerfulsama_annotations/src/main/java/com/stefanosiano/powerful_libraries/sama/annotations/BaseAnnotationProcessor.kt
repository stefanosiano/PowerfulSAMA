package com.stefanosiano.powerful_libraries.sama.annotations

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

/** Base Annotation Processor class, to be extended by other annotation processors. */
@Suppress("TooManyFunctions")
abstract class BaseAnnotationProcessor : AbstractProcessor() {

    protected lateinit var messager: Messager
    protected lateinit var filer: Filer

    @Synchronized
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        messager = processingEnv.messager
        filer = processingEnv.filer
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    protected fun getGenDir() = processingEnv.options["kapt.kotlin.generated"]!!
    protected fun getModuleDir() = getGenDir().substringBeforeLast("/build")

    protected fun logw(m: String) { messager.printMessage(Diagnostic.Kind.WARNING, "$m\n") }
    protected fun logn(m: String) { messager.printMessage(Diagnostic.Kind.NOTE, "$m\n") }
    protected fun loge(m: String) { messager.printMessage(Diagnostic.Kind.ERROR, "$m\n") }

    /** Return the kotlin type of the this variable. */
    protected fun VariableElement.toKotlinType(): TypeName {
        val tn = asType().asTypeName()
        return if (tn.isNullable) {
            tn.javaToKotlinType().copy(nullable = true)
        } else {
            tn.javaToKotlinType()
        }
    }

    /** Return the kotlin type of this typeMirror. */
    protected fun TypeMirror.toKotlinType() = asTypeName().javaToKotlinType()

    /** Transforms a java type to the corresponding kotlin type. */
    @Suppress("SpreadOperator")
    protected fun TypeName.javaToKotlinType(): TypeName =
        if (this is ParameterizedTypeName) {
            (rawType.javaToKotlinType() as ClassName)
                .parameterizedBy(*typeArguments.map { it.javaToKotlinType() }.toTypedArray())
        } else {
            val className = JavaToKotlinClassMap.INSTANCE
                .mapJavaToKotlin(FqName(toString()))?.asSingleFqName()?.asString()
            if (className == null) {
                this
            } else {
                ClassName.bestGuess(className)
            }
        }

    protected fun getKotlinType(qualifiedName: String) = getClassName(qualifiedName).javaToKotlinType()

    protected fun getClassName(qualifiedName: String) =
        ClassName(
            qualifiedName.substringBeforeLast(".", ""),
            qualifiedName.substringAfterLast(".", "")
        )

    /** Return the simpleName of the targetClass of the QueryServer annotation of this element. */
    fun TypeElement.getClassOrTargetClassName(): String = simpleName.toString()

    /**
     * Return the typeMirror of the targetClass of the QueryServer annotation of this element.
     * If the annotation is not present, or the targetClass is Unit, it returns the
     *  typeMirror of this element class.
     */
    fun TypeElement.getClassOrTargetClass(): TypeMirror = try {
        processingEnv.elementUtils.getTypeElement(qualifiedName).asType()
    } catch (e: MirroredTypeException) {
        e.typeMirror
    }

    /**
     * Check if this [Element] is a subclass of the class with the [qualifiedName],
     *  and the optional [generics] types.
     */
    fun Element.isAssignable(qualifiedName: String, generics: Int = 0): Boolean =
        isAssignable(processingEnv.elementUtils.getTypeElement(qualifiedName).asType(), generics)

    /**
     * Check if this [Element] is a subclass of the class with the [tm] [TypeMirror],
     *  and the optional [generics] types.
     */
    @Suppress("SpreadOperator")
    fun Element.isAssignable(tm: TypeMirror, generics: Int = 0): Boolean {
        val geners = (0 until generics).map { processingEnv.typeUtils.getWildcardType(null, null) }
        val declaredType = processingEnv.typeUtils.getDeclaredType(
            processingEnv.typeUtils.asElement(tm) as? TypeElement,
            *geners.toTypedArray()
        )
        return processingEnv.typeUtils.isAssignable(this.asType(), declaredType)
    }
}

/*
package com.example;	// PackageElement

public class Foo {		// TypeElement

	private int a;		// VariableElement
	private Foo other; 	// VariableElement

	public Foo () {} 	// ExecutableElement

	public void setA ( 	// ExecutableElement
	                 int newA	// TypeElement
	                 ) {}
}
 */
