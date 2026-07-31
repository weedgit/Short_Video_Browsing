import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler

object Versions {
    const val coreKtx = "1.15.0"
    const val lifecycle = "2.8.7"
    const val navigation = "2.8.5"
    const val composeBom = "2024.10.01"
    const val hilt = "2.52"
    const val hiltNavigation = "1.2.0"
    const val splashScreen = "1.0.1"
    const val media3 = "1.5.1"
    const val coil = "2.7.0"
    const val datastore = "1.1.1"
    const val retrofit = "2.11.0"
    const val okhttp = "4.12.0"
    const val coroutines = "1.9.0"
    const val room = "2.6.1"
    const val work = "2.9.1"
    const val hiltWork = "1.2.0"
    const val cameraX = "1.4.0"
}

object Libs {
    const val coreKtx = "androidx.core:core-ktx:${Versions.coreKtx}"
    const val lifecycleRuntime = "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycle}"
    const val lifecycleViewModel = "androidx.lifecycle:lifecycle-viewmodel-compose:${Versions.lifecycle}"
    const val lifecycleRuntimeCompose = "androidx.lifecycle:lifecycle-runtime-compose:${Versions.lifecycle}"
    const val navigationCompose = "androidx.navigation:navigation-compose:${Versions.navigation}"
    const val composeBom = "androidx.compose:compose-bom:${Versions.composeBom}"
    const val composeActivity = "androidx.activity:activity-compose:1.9.3"
    const val composeUi = "androidx.compose.ui:ui"
    const val composePreview = "androidx.compose.ui:ui-tooling-preview"
    const val composeMaterial3 = "androidx.compose.material3:material3"
    const val composeFoundation = "androidx.compose.foundation:foundation"
    const val composeIcons = "androidx.compose.material:material-icons-extended"
    const val splashScreen = "androidx.core:core-splashscreen:${Versions.splashScreen}"
    const val hiltAndroid = "com.google.dagger:hilt-android:${Versions.hilt}"
    const val hiltCompiler = "com.google.dagger:hilt-android-compiler:${Versions.hilt}"
    const val hiltNavigation = "androidx.hilt:hilt-navigation-compose:${Versions.hiltNavigation}"
    const val junit = "junit:junit:4.13.2"
    const val exoplayer = "androidx.media3:media3-exoplayer:${Versions.media3}"
    const val exoplayerHls = "androidx.media3:media3-exoplayer-hls:${Versions.media3}"
    const val media3Ui = "androidx.media3:media3-ui:${Versions.media3}"
    const val coilCompose = "io.coil-kt:coil-compose:${Versions.coil}"
    const val datastorePreferences = "androidx.datastore:datastore-preferences:${Versions.datastore}"
    const val retrofit = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
    const val retrofitGson = "com.squareup.retrofit2:converter-gson:${Versions.retrofit}"
    const val okhttpLogging = "com.squareup.okhttp3:logging-interceptor:${Versions.okhttp}"
    const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
    const val roomRuntime = "androidx.room:room-runtime:${Versions.room}"
    const val roomKtx = "androidx.room:room-ktx:${Versions.room}"
    const val roomCompiler = "androidx.room:room-compiler:${Versions.room}"
    const val workRuntime = "androidx.work:work-runtime-ktx:${Versions.work}"
    const val hiltWork = "androidx.hilt:hilt-work:${Versions.hiltWork}"
    const val hiltWorkCompiler = "androidx.hilt:hilt-compiler:${Versions.hiltWork}"
    const val cameraCore = "androidx.camera:camera-core:${Versions.cameraX}"
    const val cameraCamera2 = "androidx.camera:camera-camera2:${Versions.cameraX}"
    const val cameraLifecycle = "androidx.camera:camera-lifecycle:${Versions.cameraX}"
    const val cameraVideo = "androidx.camera:camera-video:${Versions.cameraX}"
    const val cameraView = "androidx.camera:camera-view:${Versions.cameraX}"
}

fun DependencyHandler.implementation(dependencyNotation: Any): Dependency? =
    add("implementation", dependencyNotation)

fun DependencyHandler.testImplementation(dependencyNotation: Any): Dependency? =
    add("testImplementation", dependencyNotation)

fun DependencyHandler.kapt(dependencyNotation: Any): Dependency? =
    add("kapt", dependencyNotation)

fun DependencyHandler.baseDependencies() {
    implementation(Libs.coreKtx)
    implementation(Libs.lifecycleRuntime)
    implementation(Libs.lifecycleViewModel)
    implementation(Libs.lifecycleRuntimeCompose)
    implementation(Libs.splashScreen)
    implementation(Libs.hiltAndroid)
    kapt(Libs.hiltCompiler)
}

fun DependencyHandler.composeDependencies() {
    implementation(platform(Libs.composeBom))
    implementation(Libs.composeActivity)
    implementation(Libs.composeUi)
    implementation(Libs.composePreview)
    implementation(Libs.composeMaterial3)
    implementation(Libs.composeFoundation)
    implementation(Libs.composeIcons)
    implementation(Libs.navigationCompose)
    implementation(Libs.hiltNavigation)
}

fun DependencyHandler.testDependencies() {
    testImplementation(Libs.junit)
}

fun DependencyHandler.media3Dependencies() {
    implementation(Libs.exoplayer)
    implementation(Libs.exoplayerHls)
    implementation(Libs.media3Ui)
}

fun DependencyHandler.coilDependencies() {
    implementation(Libs.coilCompose)
}

fun DependencyHandler.datastoreDependencies() {
    implementation(Libs.datastorePreferences)
}

fun DependencyHandler.networkDependencies() {
    implementation(Libs.retrofit)
    implementation(Libs.retrofitGson)
    implementation(Libs.okhttpLogging)
}

fun DependencyHandler.coroutinesCoreDependencies() {
    implementation(Libs.coroutinesCore)
}

fun DependencyHandler.roomDependencies() {
    implementation(Libs.roomRuntime)
    implementation(Libs.roomKtx)
    kapt(Libs.roomCompiler)
}

fun DependencyHandler.workManagerDependencies() {
    implementation(Libs.workRuntime)
    implementation(Libs.hiltWork)
    kapt(Libs.hiltWorkCompiler)
}

fun DependencyHandler.cameraXDependencies() {
    implementation(Libs.cameraCore)
    implementation(Libs.cameraCamera2)
    implementation(Libs.cameraLifecycle)
    implementation(Libs.cameraVideo)
    implementation(Libs.cameraView)
}

fun DependencyHandler.projectModule(path: String) {
    implementation(project(mapOf("path" to path)))
}
