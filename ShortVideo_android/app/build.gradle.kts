import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val apiBaseUrl = localProperties.getProperty("API_BASE_URL", "http://10.0.2.2:3000/")
    .let { url ->
        require(url.endsWith("/")) { "API_BASE_URL must end with a trailing slash: $url" }
        url
    }

android {
    namespace = AppConfig.applicationId
    compileSdk = AppConfig.compileSdk

    defaultConfig {
        applicationId = AppConfig.applicationId
        minSdk = AppConfig.minSdk
        targetSdk = AppConfig.targetSdk
        versionCode = AppConfig.versionCode
        versionName = AppConfig.versionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    projectModule(":core")
    projectModule(":domain")
    projectModule(":data")
    projectModule(":common:theme")
    projectModule(":common:composable")
    projectModule(":feature:home")
    projectModule(":feature:discover")
    projectModule(":feature:upload")
    projectModule(":feature:inbox")
    projectModule(":feature:profile")
    projectModule(":feature:auth")
    projectModule(":feature:onboarding")
    projectModule(":feature:settings")

    baseDependencies()
    composeDependencies()
    media3Dependencies()
    workManagerDependencies()
    testDependencies()
}
