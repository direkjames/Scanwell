# Extra rules for the release build's code shrinker (R8).

# Google ML Kit (the scanner and the text reader) finds its own parts by name when it starts,
# from a list in the app's manifest. The shrinker cannot see that, so it removed or renamed
# those parts and the release app crashed on launch. These rules keep them as they are.
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }

# PDFBox (PDF encryption and compression) loads some classes by name and mentions optional
# image decoders that are not bundled. Keep it intact and silence the optional warnings.
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn com.gemalto.jp2.**
-dontwarn org.bouncycastle.**
-dontwarn org.spongycastle.**
