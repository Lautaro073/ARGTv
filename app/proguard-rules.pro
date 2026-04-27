# Proguard rules for ARGTv

# Keep Moshi
-keep class com.argtv.data.model.** { *; }
-keepclassmembers class com.argtv.data.model.** { *; }
-keep class com.squareup.moshi.** { *; }
-keepinterface com.squareup.moshi.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Keep Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Media3/ExoPlayer
-keep class androidx.media3.** { *; }