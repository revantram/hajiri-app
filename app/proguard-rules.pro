# Proguard / R8 keep rules for Hajiri app
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Firestore POJOs / DTOs (serialized & deserialized via reflection by Firestore SDK)
-keep class com.example.hajiri.model.** { *; }
-keepclassmembers class com.example.hajiri.model.** { *; }

# Keep Firestore mapping annotations and annotated members
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
}
-keep @com.google.firebase.firestore.IgnoreExtraProperties class * { *; }

# Keep BroadcastReceivers and AppWidgetProvider instantiated by Android framework
-keep class com.example.hajiri.util.DailyReminderReceiver { *; }
-keep class com.example.hajiri.util.DailyReminderRescheduleReceiver { *; }
-keep class com.example.hajiri.widget.HajiriWidgetReceiver { *; }

# Firebase, Play Services & Credential Manager
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn androidx.credentials.**
-dontwarn com.google.android.libraries.identity.googleid.**
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.playservices.** { *; }

# Strip verbose debug logs in release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
