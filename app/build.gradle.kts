plugins {
    alias(libs.plugins.androidApp)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.kotlinSerialization)
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
}

android {
    namespace = "com.danylo.seriesdiary"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.danylo.seriesdiary"
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    add("ksp", "androidx.room:room-compiler:$room_version")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")

    // ЛР №12: геолокація через Fused Location Provider
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // ЛР №12: завантаження фото з файлової системи у Compose
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ЛР №13: модульні тести (Завдання 1) — лише coroutines-test, без mockk (fake-и вручну)
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // ЛР №13: UI-тести (Завдання 2)
    // Compose ui-test жорстко залежить від Espresso (EspressoLink), тож espresso-core
    // має бути на classpath. Espresso 3.6.1 несумісний з API 35/36 (InputManager.getInstance),
    // тому UI-тест запускати на емуляторі API ≤ 34 (Android 14).
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}