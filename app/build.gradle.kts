plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Compose plugin for Kotlin 2.x (no composeOptions block needed)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    // ✅ Use KSP with the SAME Kotlin version
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
    // ❌ REMOVE kapt (KAPT is not compatible with Kotlin 2.x / K2)
    // id("kotlin-kapt")
}

android {
    namespace = "com.example.todo_app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.todo_app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { compose = true }

    // ❌ Not needed with the compose plugin:
    // composeOptions { kotlinCompilerExtensionVersion = "..." }

    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // ✅ Use Java 17 for modern Compose/Kotlin toolchains
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    // (Optional, but nice)
    // kotlin { jvmToolchain(17) }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core & lifecycle
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Compose UI + Material3 + Navigation
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.compose.material:material-icons-extended")

    // Room -> ✅ switch to KSP
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Markdown (optional)
    implementation("org.commonmark:commonmark:0.22.0")

    // (Optional: only if you use classic Views)
    implementation("com.google.android.material:material:1.12.0")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
