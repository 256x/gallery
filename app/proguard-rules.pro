-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembernames class * {
    @dagger.hilt.* <methods>;
    @javax.inject.* <fields>;
}

-keep class * extends androidx.lifecycle.ViewModel { *; }

-keep class fumi.day.literalbunko.data.** { *; }
-keep class fumi.day.literalbunko.domain.** { *; }

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

-dontwarn com.google.errorprone.annotations.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
