import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.deepintv.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.deepintv.launcher"
        minSdk = 26
        targetSdk = 34
        versionCode = 6
        versionName = "0.6"
    }

    // The published APKs are signed with the maintainer's own key, described by keystore.properties
    // (never committed, see keystore.properties.example). Without that file, e.g. in a fresh clone,
    // release builds fall back to the debug key so anyone can still build and install.
    val keystoreFile = rootProject.file("keystore.properties")
    val releaseSigning = if (keystoreFile.exists()) {
        val props = Properties().apply { keystoreFile.inputStream().use { load(it) } }
        signingConfigs.create("release") {
            storeFile = file(props.getProperty("storeFile"))
            storePassword = props.getProperty("storePassword")
            keyAlias = props.getProperty("keyAlias")
            keyPassword = props.getProperty("keyPassword")
        }
    } else {
        signingConfigs.getByName("debug")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = releaseSigning
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.runtime:runtime")
    // Vector icons for the control center (replaces the emoji glyphs some TVs' system fonts
    // don't carry, confirmed on the TCL G10 — real vector art renders everywhere).
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.tv:tv-foundation:1.0.0-alpha11")
    implementation("androidx.tv:tv-material:1.0.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    // Only the encoder: the pairing screen draws the QR itself, no camera or Android UI parts.
    implementation("com.google.zxing:core:3.5.3")
    // Installs src/main/baseline-prof.txt so the first launch runs pre-compiled instead of
    // interpreted, which is most of the cold-start lag on a 1 GB TV.
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")

    testImplementation("junit:junit:4.13.2")
    // The JVM has no org.json of its own (android.jar's copy is a stub that throws).
    testImplementation("org.json:json:20231013")
}
