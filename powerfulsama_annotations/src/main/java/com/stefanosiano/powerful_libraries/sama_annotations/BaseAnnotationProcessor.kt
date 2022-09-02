package com.stefanosiano.powerful_libraries.sama_annotations

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.annotations.Nullable
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.internal.impl.types.TypeUtils

abstract class BaseAnnotationProcessor : AbstractProcessor() {

    lateinit var messager: Messager
    lateinit var filer: Filer

    @Synchronized
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        messager = processingEnv.messager
        filer = processingEnv.filer
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()


    protected fun getGenDir() = processingEnv.options["kapt.kotlin.generated"]!!
    protected fun getModuleDir() = getGenDir().substringBeforeLast("/build")


    protected fun logw(message: String) { messager.printMessage(Diagnostic.Kind.WARNING, "$message\n") }
    protected fun logn(message: String) { messager.printMessage(Diagnostic.Kind.NOTE, "$message\n") }
    protected fun loge(message: String) { messager.printMessage(Diagnostic.Kind.ERROR, "$message\n") }

    /** Return the kotlin type of the this variable. */
    protected fun VariableElement.toKotlinType(): TypeName {
        val tn = asType().asTypeName()
        if(tn.isNullable) return tn.javaToKotlinType().copy(nullable = true)
        else return tn.javaToKotlinType()
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
        if (className == null) this
        else ClassName.bestGuess(className)
    }

    protected fun getKotlinType(qualifiedName: String) = getClassName(qualifiedName).javaToKotlinType()

    protected fun getClassName(qualifiedName: String) =
        ClassName(qualifiedName.substringBeforeLast(".", ""), qualifiedName.substringAfterLast(".", ""))


    /** Return the simpleName of the targetClass of the QueryServer annotation of this element.
     * If the annotation is not present, or the targetClass is Unit, it returns the simple p of this element class. */
    fun TypeElement.getClassOrTargetClassName(): String = simpleName.toString()

    /** Return the typeMirror of the targetClass of the QueryServer annotation of this element.
     * If the annotation is not present, or the targetClass is Unit, it returns the typeMirror of this element class. */
    fun TypeElement.getClassOrTargetClass(): TypeMirror = try { processingEnv.elementUtils.getTypeElement(qualifiedName).asType() } catch (e: MirroredTypeException) { e.typeMirror }

    fun Element.isAssignable(qualifiedName: String, generics: Int = 0): Boolean =
        isAssignable(processingEnv.elementUtils.getTypeElement(qualifiedName).asType(), generics)

    fun Element.isAssignable(tm: TypeMirror, generics: Int = 0): Boolean {
        val geners = (0 until generics).map { processingEnv.typeUtils.getWildcardType(null, null) }.toTypedArray()
        val declaredType = processingEnv.typeUtils.getDeclaredType(processingEnv.typeUtils.asElement(tm) as? TypeElement, *geners)
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