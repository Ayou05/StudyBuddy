# Add project specific ProGuard rules here.
-keepattributes *Annotation*

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }

# kotlinx.serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep,includedescriptorclasses class com.studybuddy.v2.**$$serializer { *; }
-keepclassmembers class com.studybuddy.v2.** {
    *** Companion;
}
-keepclasseswithmembers class com.studybuddy.v2.** {
    kotlinx.serialization.KSerializer serializer(...);
}
