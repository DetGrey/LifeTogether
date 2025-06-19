import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.gms.google-services")
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("com.google.devtools.ksp")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    alias(libs.plugins.kotlinCompose)
}

android {
    namespace = "com.example.lifetogether"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.lifetogether"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val adminList: String = gradleLocalProperties(rootDir, providers).getProperty("adminList")
        buildConfigField("String", "ADMIN_LIST", adminList)

        buildFeatures {
            buildConfig = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21) // Ensure compatibility with Kotlin 1.9.0
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

ktlint {
    android = true
    ignoreFailures = false
    version = "0.50.0"
    reporters {
        reporter(ReporterType.CHECKSTYLE)
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
    implementation(libs.firebase.dataconnect)
    implementation(libs.androidx.compose.material)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.androidx.exifinterface)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // View model
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Navigation
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.multidex)

    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))
    // Declare the dependency for the Cloud Firestore library
    implementation(libs.firebase.firestore)
    // Declare the dependency for the Firebase Storage library
    implementation(libs.firebase.storage)
    // Add the dependency for the Firebase Authentication library
    implementation(libs.firebase.auth)
    // Add the dependency for the Firebase Messaging library
    implementation(libs.firebase.messaging)
    implementation(libs.google.api.client)
    implementation(libs.google.auth.library.oauth2.http)
    implementation(libs.google.http.client.gson)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(kotlin("reflect"))

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Gson
    implementation(libs.gson)

    // Coil - image loading and processing
    implementation(libs.coil)
    implementation(libs.coil.compose)

    // Notification permission
    implementation(libs.accompanist.permissions)
}

// 1. Create the rename task: Post-Build Rename APK file
tasks.register<Copy>("renameReleaseApk") {
    val versionName = android.defaultConfig.versionName ?: "unknown"
    val buildType = "release"
    val apkName = "app-$buildType.apk"
    val newName = "LifeTogether-$versionName.apk"

    from(layout.buildDirectory.dir("outputs/apk/$buildType")) {
        include(apkName)
        rename(apkName, newName)
    }
    into(layout.buildDirectory.dir("outputs/renamed-apk"))
}

// 2. Hook it automatically to run after the APK is built
afterEvaluate {
    tasks.findByName("assembleRelease")?.finalizedBy(tasks.named("renameReleaseApk"))
        ?: logger.warn("assembleRelease task not found — renameReleaseApk will not run automatically.")
}
