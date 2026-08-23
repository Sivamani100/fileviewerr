# Apache POI / Tika / Batik - Ignore missing optional dependencies
-dontwarn org.apache.poi.**
-dontwarn org.apache.tika.**
-dontwarn org.apache.batik.**
-dontwarn javax.xml.stream.**
-dontwarn com.sun.xml.internal.**
-dontwarn org.bouncycastle.**
-dontwarn com.microsoft.schemas.**
-dontwarn org.openxmlformats.schemas.**
-dontwarn org.etsi.uri.**
-dontwarn org.w3.x2000.x09.xmldsig.**

# Keep iText classes
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# Keep our engine
-keep class com.arkio.officeengine.** { *; }
