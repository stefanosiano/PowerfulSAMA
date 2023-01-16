# PowerfulSAMA
Android library that simplifies the development of mvvm apps
Still in development (so no readme at the moment for its features), but stable and already used in production on several apps.

PowerfulSAMA (Simplified Android Mvvm Architecture) is a sort of framework for Android development.  
It contains a lot of base classes and utilities that provide a huge speed up in app development.

It's heavily based on LiveData and ViewModel architecture components, Mvvm, Data Binding and reactive programming.

It contains few annotations to generate extension functions to provide default (and most of the time exhaustive) implementations of methods.

Utilities include management of:
- permissions
- messages (toasts, alert dialogs, etc.)
- resources (strings, colors, etc.)
- notifications
- messages

Gradle
------
  
```
dependencies {
    implementation 'io.github.stefanosiano.powerful_libraries:sama:0.5.00' // Put this line into module's build.gradle
    compileOnly 'io.github.stefanosiano.powerful_libraries:sama_annotations:0.5.00' // Put this line for extended funcionalities
    kapt 'io.github.stefanosiano.powerful_libraries:sama_annotations:0.5.00' // Put this line for extended funcionalities
}
```
  