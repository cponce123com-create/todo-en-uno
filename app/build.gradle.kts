plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "pe.todoenuno.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "pe.todoenuno.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.5.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:catalog"))
    implementation(project(":feature:utilities"))
    implementation(project(":feature:math-tools"))
    implementation(project(":feature:sensor-tools"))
    implementation(project(":feature:location-tools"))
    implementation(project(":feature:audio"))
    implementation(project(":feature:recorder"))
    implementation(project(":feature:finance"))
    implementation(project(":feature:camera"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.serialization.json)
    debugImplementation(libs.compose.tooling)
    testImplementation(libs.junit)
}
