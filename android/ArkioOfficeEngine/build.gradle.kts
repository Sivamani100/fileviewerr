plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    namespace = "com.arkio.officeengine"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,DEPENDENCIES,LICENSE,NOTICE,INDEX.LIST}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            )
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("androidx.multidex:multidex:2.0.1")
    
    // APACHE POI
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.apache.poi:poi:5.2.5") {
        exclude(group = "commons-logging", module = "commons-logging")
        exclude(group = "log4j", module = "log4j")
    }
    implementation("org.apache.poi:poi-ooxml:5.2.5") {
        exclude(group = "commons-logging", module = "commons-logging")
        exclude(group = "log4j", module = "log4j")
        exclude(group = "org.apache.xmlbeans", module = "xmlbeans")
    }
    implementation("org.apache.poi:poi-ooxml-lite:5.2.5")
    implementation("org.apache.poi:poi-scratchpad:5.2.5") {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("commons-codec:commons-codec:1.16.0")
    implementation("org.apache.commons:commons-compress:1.25.0")
    implementation("com.github.virtuald:curvesapi:1.08")
    implementation("com.zaxxer:SparseBitSet:1.3")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("com.github.tony19:logback-android:3.0.0")
    
    // ITEXT7
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("com.itextpdf:kernel:7.2.5")
    implementation("com.itextpdf:layout:7.2.5")
    implementation("com.itextpdf:io:7.2.5")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
}
