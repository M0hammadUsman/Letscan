plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.navigation.safeargs)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.qrscanner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.qrscanner"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    // CameraX
    implementation (libs.camerax.core)
    implementation (libs.camerax.camera2)
    implementation (libs.camerax.lifecycle)
    implementation (libs.camerax.view)
    // QR Code Scanning
    implementation (libs.mlkit.barcode.scanning)
    // Room Database
    implementation (libs.room.runtime)
    implementation (libs.room.ktx)
    ksp(libs.androidx.room.compiler.v250)
    // Navigation Component
    implementation (libs.navigation.fragment.ktx)
    implementation( libs.navigation.ui.ktx)
    // WebView
    implementation (libs.webkit)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}