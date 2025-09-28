# Add project specific ProGuard rules here.
# These rules are appended to the ones defined in
# ${sdk.dir}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html
#
# Add any rules specific to Compose, coroutines, serialization, etc. below.
-keep class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }
-dontwarn kotlinx.serialization.**
