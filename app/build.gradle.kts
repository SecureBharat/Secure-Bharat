plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services") version "4.4.3"
    id("org.jetbrains.kotlin.kapt") // Add this line
}

android {
    namespace = "com.example.paisacheck360"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.paisacheck360"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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



    implementation ("com.github.bumptech.glide:glide:4.16.0")
    kapt ("com.github.bumptech.glide:compiler:4.16.0")



    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")

    implementation("com.google.android.material:material:1.11.0")

    // OkHttp – for networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

// Coroutines – for async code
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")




    // ✅ Firebase BoM – controls all versions
    implementation(platform("com.google.firebase:firebase-bom:32.7.3"))

    // ✅ Firebase Realtime Database
    implementation("com.google.firebase:firebase-database-ktx")

    // Optional – Analytics (only if needed)
    implementation("com.google.firebase:firebase-analytics")

    // Retrofit for future API stuff
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Core AndroidX libs
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}