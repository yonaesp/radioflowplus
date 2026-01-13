plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}

android {
    namespace = "com.radioandroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.radioandroid"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/debug-release.keystore")
            storePassword = "radioflow123"
            keyAlias = "radioflow"
            keyPassword = "radioflow123"
        }
    }

    buildTypes {
        debug {
            buildConfigField("Boolean", "PREMIUM_ENABLED", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("Boolean", "PREMIUM_ENABLED", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        // Enable Strong Skipping mode for better Compose performance
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xcontext-receivers"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android - Updated to latest 2024/2025 versions
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7") // For collectAsStateWithLifecycle
    implementation("androidx.activity:activity-compose:1.9.3")
    
    // Immutable Collections for Compose stability
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.7")
    
    // Compose - Latest BOM December 2024
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // ExoPlayer/Media3 - Using 1.4.1 due to AAC tunneling bug in 1.5.0
    // Bug: STATE_ENDED incorrectly fired every 2-4 seconds on AAC live streams
    // See: https://github.com/androidx/media/issues (AAC tunneling regression)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")  // Required for HLS (.m3u8) streams
    implementation("androidx.media3:media3-exoplayer-dash:1.4.1") // Required for DASH streams
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.4.1") // Robust networking
    implementation("androidx.media3:media3-cast:1.4.1") // Chromecast support
    
    // Google Cast Framework
    implementation("com.google.android.gms:play-services-cast-framework:22.0.0")
    
    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.7.0")
    
    // Legacy media support for notifications
    implementation("androidx.media:media:1.7.0")

    // Jetpack Glance for Widgets
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")
    
    // Dynamic Colors
    implementation("androidx.palette:palette-ktx:1.0.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
