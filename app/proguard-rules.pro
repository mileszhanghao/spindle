# Retrofit, OkHttp, kotlinx.serialization, Room, Hilt and Media3 ship their own
# consumer rules. Keep the shared wire models' serializers explicitly as well.
-keep,includedescriptorclasses class com.mileszhang.spindle.model.**$$serializer { *; }
-keepclassmembers class com.mileszhang.spindle.model.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
