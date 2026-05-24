plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)

    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

android {
    namespace = "com.example.pulsar"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.pulsar"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildFeatures {
        compose = true
    }

    defaultConfig {
        applicationId = "com.example.pulsar"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // Filter ABIs to reduce APK size.
            // arm64-v8a covers almost all modern physical Android devices (64-bit).
            // Removing armeabi-v7a saves ~45MB of native library bloat (ffmpeg, python, etc).
            abiFilters.addAll(listOf("arm64-v8a"))
        }
    }


}

dependencies {
    // --- Existing ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.palette.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // --- 2. Room ---
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // --- 3. WorkManager & Hilt Integration ---
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler) // Essential for injecting dependencies into Workers

    // --- 4. DataStore ---
    implementation(libs.datastore.preferences)

    // --- 5. Navigation & ViewModel Compose ---
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // --- 6. Coil 3 ---
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // --- 7. Kotlinx Serialization ---
    implementation(libs.kotlinx.serialization.json)

    // --- 8. FFmpegKit removed (unused and large) ---

    // --- 9. Icons ---
    implementation(libs.androidx.compose.material.icons.extended)

    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    implementation(libs.work.runtime.ktx)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    val youtubedlAndroid = "0.18.1"
    implementation("io.github.junkfood02.youtubedl-android:library:$youtubedlAndroid")


    // Optional but highly recommended since we plan to mux video/audio later!
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:$youtubedlAndroid")
    implementation("io.github.junkfood02.youtubedl-android:aria2c:$youtubedlAndroid")
}
ksp {
    arg("room.generateKotlin", "true")
}