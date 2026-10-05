# Proguard rules for AmozVz
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
-keep class com.amozvz.app.data.models.** { *; }
-keep class com.google.gson.** { *; }
