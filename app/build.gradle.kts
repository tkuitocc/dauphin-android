plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.dauphin"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.dauphin.android"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val aesKey = System.getenv("AES_KEY") ?: project.findProperty("AES_KEY")?.toString() ?: "" // 32 chars for AES-256
        val aesIv = System.getenv("AES_IV") ?: project.findProperty("AES_IV")?.toString() ?: "" // 16 chars for CBC

        buildConfigField("String", "AES_KEY", "\"$aesKey\"")
        buildConfigField("String", "AES_IV", "\"$aesIv\"")
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.work.runtime.ktx)
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.graphics.shapes)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.viewbinding)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.compose.animation.graphics)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material)

    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.material.ripple)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.compose.ui.googlefonts)

    implementation(libs.androidx.emoji2.views)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.fragment.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.coordinator.layout)
    implementation(libs.google.android.material)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    implementation(libs.androidx.window.core)

    implementation(libs.accompanist.theme.adapter.appcompat)
    implementation(libs.accompanist.theme.adapter.material3)
    implementation(libs.accompanist.theme.adapter.material)

    implementation(libs.accompanist.permissions)

    implementation(libs.coil.kt.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewModelCompose)

    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.recyclerview)

    implementation(libs.googlemaps.compose)
    implementation(libs.googlemaps.maps)

    implementation(libs.hilt.android)
    implementation(libs.glide.compose)

    implementation(libs.okhttp)
    implementation(libs.androidx.datastore.preferences)

    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)

    androidTestImplementation(composeBom)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.tooling)

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4.accessibility)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.glance.testing)
    androidTestImplementation(libs.androidx.glance.appwidget.testing)
}
