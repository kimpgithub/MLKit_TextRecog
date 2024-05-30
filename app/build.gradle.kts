plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.mlkit_textrecog"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mlkit_textrecog"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // To recognize Latin script
    implementation ("com.google.mlkit:text-recognition:16.0.0")
    // To recognize Chinese script
    implementation ("com.google.mlkit:text-recognition-chinese:16.0.0")
    // To recognize Devanagari script
    implementation ("com.google.mlkit:text-recognition-devanagari:16.0.0")
    // To recognize Japanese script
    implementation ("com.google.mlkit:text-recognition-japanese:16.0.0")
    // To recognize Korean script
    implementation ("com.google.mlkit:text-recognition-korean:16.0.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation ("com.google.mlkit:translate:17.0.2")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.0")
    val nav_version = "2.7.7"
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation("androidx.navigation:navigation-ui-ktx:$nav_version")
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation(libs.firebase.analytics)
    implementation ("com.google.firebase:firebase-auth")
    implementation ("com.google.firebase:firebase-firestore")
    implementation ("com.google.firebase:firebase-storage")
    implementation ("com.google.firebase:firebase-messaging:23.0.5")
    implementation ("org.tensorflow:tensorflow-lite:2.9.0")





}