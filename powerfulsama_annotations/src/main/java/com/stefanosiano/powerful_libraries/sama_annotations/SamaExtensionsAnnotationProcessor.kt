package com.stefanosiano.powerful_libraries.sama_annotations

import androidx.room.Ignore
import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.*
import javax.lang.model.util.ElementFilter
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmWildcard

class SamaExtensionsAnnotationProcessor : BaseAnnotationProcessor() {

    private val imports = ArrayList<Pair<String, String>>()
    override fun getSupportedAnnotationTypes(): Set<String> = setOf(SamaExtensions::class.java.name, JvmField::class.java.name)

    override fun process(set: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {

        val annotation = roundEnv.getElementsAnnotatedWith(SamaExtensions::class.java)
            .filter { it.kind == ElementKind.CLASS }
            .map { it.getAnnotation(SamaExtensions::class.java) }.firstOrNull() ?: return false


        val genDir = getGenDir()

        val generatedPackage = "com.stefanosiano.powerful_libraries.sama.generatedextensions"
        val generatedSimpleName = "GeneratedSamaExtensions"

        val functions = ArrayList<FunSpec>()
        functions.addAll(addDefaultContentEquals(roundEnv))
        functions.addAll(addDefaultRestore(roundEnv))

        val file = FileSpec.builder(generatedPackage, generatedSimpleName)
        imports.forEach { i -> file.addImport(i.first, i.second) }
        functions.forEach { f -> file.addFunction(f) }

        file.build().writeTo(File(genDir))

        return false
    }



    /** Add SamaListItems default contentEquals() implementations as extension functions */
    private fun addDefaultContentEquals(roundEnv: RoundEnvironment): ArrayList<FunSpec> {
        val functions = ArrayList<FunSpec>()

        val samaListItemType = processingEnv.elementUtils.getTypeElement("com.stefanosiano.powerful_libraries.sama.view.SamaListItem")
        roundEnv.rootElements.filter { it.kind == ElementKind.CLASS && it.isAssignable(samaListItemType.asType()) && !it.modifiers.contains(Modifier.ABSTRACT) }.forEach { cls ->
            if (cls !is TypeElement) return@forEach

            val function = FunSpec.builder("defaultContentEquals").receiver(getKotlinType(cls.qualifiedName.toString()))

            function.addParameter(ParameterSpec.builder("other", samaListItemType.asType().toKotlinType()).build())
                .addKdoc(" %S ", "This function compares all fields in this class (excluding inherited fields) not annotated with room's Ignore annotation")
                .returns(Boolean::class)
                .addStatement("val ret = when {")
                .addStatement("\tother !is ${cls.qualifiedName} -> false")

            cls.enclosedElements.filter { it as? VariableElement != null && it.getAnnotation(Ignore::class.java) == null && it.getAnnotation(IgnoreField::class.java) == null  && !it.modifiers.contains(Modifier.STATIC) }.map { it as VariableElement }.forEach { v ->
                function.addStatement("\tthis.${v.simpleName} != other.${v.simpleName} -> false")
            }

            function.addStatement("\telse -> true")
                .addStatement("}")
                .addStatement("return ret")

            functions.add(function.build())
        }
        return functions
    }


    /** Add SamaListItems default contentEquals() implementations as extension functions */
    private fun addDefaultRestore(roundEnv: RoundEnvironment): ArrayList<FunSpec> {
        val functions = ArrayList<FunSpec>()

        val dialogFragmentType = processingEnv.elementUtils.getTypeElement("com.stefanosiano.powerful_libraries.sama.view.SamaDialogFragment")
        roundEnv.rootElements.filter { it.kind == ElementKind.CLASS && it.isAssignable(dialogFragmentType.asType()) && !it.modifiers.contains(Modifier.ABSTRACT) }.forEach { cls ->
            if (cls !is TypeElement) return@forEach

            val function = FunSpec.builder("defaultRestore").receiver(getKotlinType(cls.qualifiedName.toString()))

            function.addParameter(ParameterSpec.builder("oldDialog", dialogFragmentType.asType().toKotlinType()).build())
                .addKdoc(" %S ", "Restore previous data from events like device rotating when a dialog is shown. The [dialogFragment] in [oldDialog] is null")
            function.addStatement("\tif(oldDialog !is ${cls.simpleName}) return")

            cls.enclosedElements.filter { it as? VariableElement != null && it.getAnnotation(Ignore::class.java) == null && it.getAnnotation(IgnoreField::class.java) == null && !it.modifiers.contains(Modifier.STATIC) }.map { it as VariableElement }.forEach { v ->
                when {
                    v.simpleName.toString().contains("$") -> {}
                    v.isAssignable("androidx.databinding.ObservableInt") ||
                    v.isAssignable("androidx.databinding.ObservableShort") ||
                    v.isAssignable("androidx.databinding.ObservableLong") ||
                    v.isAssignable("androidx.databinding.ObservableFloat") ||
                    v.isAssignable("androidx.databinding.ObservableDouble") ||
                    v.isAssignable("androidx.databinding.ObservableBoolean") ||
                    v.isAssignable("androidx.databinding.ObservableByte") ||
                    v.isAssignable("androidx.databinding.ObservableField", 1) ->
                        function.addStatement("\tthis.${v.simpleName}.set(oldDialog.${v.simpleName}.get())")

                    v.modifiers.contains(Modifier.FINAL) ->
                        function.addStatement("\t//this.${v.simpleName} = oldDialog.${v.simpleName} --> Commented due to val field")
                    else -> function.addStatement("\tthis.${v.simpleName} = oldDialog.${v.simpleName}")
                }
            }

            functions.add(function.build())
        }
        return functions
    }


/*

/** Observes a sharedPreference until the ViewModel is destroyed, using a custom live data, and transforms it into an observable field. Does not update the observable if the value of the preference is null */
fun <T> SamaViewModel<*>.observeAsOf(preference: PowerfulPreference<T>): ObservableField<T> {
	val observable = ObservableField<T>()
	observe(preference.asLiveData()) { observable.set(it ?: return@observe) }
	observable.set(preference.get() ?: return observable)
	return observable
}
*/
}