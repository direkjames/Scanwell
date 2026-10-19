plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    // The Kotlin package the code lives in. Leave this as it is.
    namespace = "com.scanwell.app"
    compileSdk = 37

    defaultConfig {
        // The app's unique ID on phones and on Google Play.
        // Change this to something of your own BEFORE your first upload
        // (for example "com.yourname.scanwell"). It can never change after that.
        applicationId = "com.scanwell.app"

        minSdk = 26        // oldest Android version supported (Android 8.0)
        targetSdk = 36     // Google Play requires 36 or higher for new apps

        // Raise versionCode by 1 for every upload to Google Play.
        versionCode = 2
        versionName = "1.1"
    }

    buildTypes {
        release {
            // Shrinks and optimizes the app for release builds.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.mlkit.document.scanner)
    implementation(libs.mlkit.text.recognition)

    implementation(libs.pdfbox.android)

    testImplementation(libs.junit)
}
