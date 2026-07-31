plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.shortvideo.composable"
    compileSdk = AppConfig.compileSdk

    defaultConfig {
        minSdk = AppConfig.minSdk
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
    implementation(project(":common:theme"))
    implementation(project(":domain"))
    implementation(platform(Libs.composeBom))
    implementation(Libs.composeMaterial3)
    implementation(Libs.composeUi)
    implementation(Libs.composeFoundation)
    implementation(Libs.composeIcons)
    media3Dependencies()
    coilDependencies()
}
