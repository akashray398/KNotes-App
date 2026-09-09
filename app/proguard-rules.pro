# KNotes - ProGuard/R8 Rules

# Hilt
-keep class com.example.knotes.** { *; }
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep interface * extends androidx.room.Dao

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class com.example.knotes.data.entity.** { *; }
-keep class com.example.knotes.domain.model.** { *; }
-keep class com.example.knotes.data.repository.impl.BackupRepositoryImpl$BackupData { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-keepattributes SourceFile,LineNumberTable
-keep public class com.google.firebase.firestore.FieldValue { *; }

# Generative AI (Gemini)
-keep class com.google.ai.client.generativeai.** { *; }

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeVisibleTypeAnnotations
-keepclassmembers class retrofit2.BuiltInConverters$ToStringConverter { *; }
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**
-dontwarn retrofit2.Platform$Java8

# Markdown (Markwon)
-keep class io.noties.markwon.** { *; }

# Android X
-keep class androidx.lifecycle.** { *; }
-keep class androidx.navigation.** { *; }
