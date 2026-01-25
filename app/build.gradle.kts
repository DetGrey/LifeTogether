import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("com.google.gms.google-services")
    alias(libs.plugins.ktlint)
    kotlin("plugin.serialization") version "2.1.0"
}

android {
    namespace = "com.example.lifetogether"
    compileSdk = 36

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

    // Define which keys you want to export
    val localPropertiesKeys =
        mapOf(
            "R2_ACCOUNT_ID" to "r2.accountId",
            "R2_BUCKET_NAME" to "r2.bucketName",
            "R2_ACCESS_KEY_ID" to "r2.accessKeyId",
            "R2_SECRET_ACCESS_KEY" to "r2.secretAccessKey",
            "R2_PUBLIC_DOMAIN" to "r2.publicDomain",
            "ADMIN_LIST" to "adminList",
        )
    // Loop through them and apply them to defaultConfig
    for ((configName, propKey) in localPropertiesKeys) {
        val propValue = localProps.getProperty(propKey) ?: ""
        defaultConfig.buildConfigField("String", configName, "\"$propValue\"")
    }

    defaultConfig {
        applicationId = "com.example.lifetogether"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.4.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildFeatures {
            compose = true
            buildConfig = true
            resValues = true
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.dataconnect)
    implementation(libs.androidx.compose.material)
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
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.gson)
    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.zoomable.image.coil)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.s3)
    implementation(libs.aws.config)
    implementation(libs.http.client.engine.okhttp)
    constraints {
        // Force compatible gRPC version for Firebase Firestore 26.1.0
        // Using 1.65.1 which is tested and compatible
        val grpcVersion = "1.65.1"
        implementation("io.grpc:grpc-okhttp:$grpcVersion")
        implementation("io.grpc:grpc-android:$grpcVersion")
        implementation("io.grpc:grpc-protobuf-lite:$grpcVersion")
        implementation("io.grpc:grpc-stub:$grpcVersion")
        implementation("io.grpc:grpc-api:$grpcVersion")
        implementation("io.grpc:grpc-core:$grpcVersion")
    }
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

ktlint {
    android = true
    ignoreFailures = false
    reporters {
        reporter(ReporterType.CHECKSTYLE)
    }
}
subprojects {
    plugins.withId("org.jlleitschuh.gradle.ktlint") {
        tasks.named("ktlintCheck") {
            dependsOn("ktlintFormat")
        }
    }
}
// This creates a global "fix all and check all" command
tasks.register("ktlint") {
    group = "verification"
    // Collects all ktlintCheck tasks from all submodules
    dependsOn(subprojects.map { "${it.path}:ktlintCheck" })
}

afterEvaluate {
    tasks.findByName("assembleDebug")?.finalizedBy(tasks.named("renameDebugApk"))
        ?: logger.warn("assembleDebug task not found — renameDebugApk will not run automatically.")
}
