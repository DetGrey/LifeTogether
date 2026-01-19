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

    // --- Load Signing Properties ---
    val localProps = gradleLocalProperties(rootDir, providers)

    signingConfigs {
        create("release") {
            // This assumes your .jks file is in the 'app' folder
            storeFile = file(localProps.getProperty("signing.storeFile") ?: "")
            storePassword = localProps.getProperty("signing.storePassword")
            keyAlias = localProps.getProperty("signing.keyAlias")
            keyPassword = localProps.getProperty("signing.keyPassword")
        }
    }

    defaultConfig {
        applicationId = "com.example.lifetogether"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val adminList: String = localProps.getProperty("adminList") ?: ""
        buildConfigField("String", "ADMIN_LIST", adminList)

        buildFeatures {
            buildConfig = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Use the signing configuration we defined above
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }

        // --- Debug variant that can live alongside Release ---
        getByName("debug") {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "LifeTogether (DEV)")
        }
    }

    lint {
        // Disables the specific check that is crashing the build
        disable += "NullSafeMutableLiveData"
        // Also a good idea for hobby projects to prevent builds from failing
        // just because of minor UI warnings
        abortOnError = false
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
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
    implementation(libs.androidx.compose.foundation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.multidex)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation(libs.google.api.client)
    implementation(libs.google.auth.library.oauth2.http)
    implementation(libs.google.http.client.gson)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(kotlin("reflect"))
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.gson)
    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
}

tasks.register<Copy>("renameDebugApk") {
    val versionName = android.defaultConfig.versionName ?: "unknown"
    val buildType = "debug"
    val apkName = "app-$buildType.apk"
    val newName = "LifeTogether-$versionName.apk"

    from(layout.buildDirectory.dir("outputs/apk/$buildType")) {
        include(apkName)
        rename(apkName, newName)
    }
    into(layout.buildDirectory.dir("outputs/renamed-apk"))
}

afterEvaluate {
    tasks.findByName("assembleDebug")?.finalizedBy(tasks.named("renameDebugApk"))
        ?: logger.warn("assembleDebug task not found — renameDebugApk will not run automatically.")
}
