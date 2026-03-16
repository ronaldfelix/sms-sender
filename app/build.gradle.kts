plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.aref.smssender"
    compileSdk {
        version = release(36)
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "org.aref.smssender"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("int", "APP_PORT", "${extra["APP_PORT"]}")
        buildConfigField("String", "ENDPOINT", "${extra["ENDPOINT"]}")
        buildConfigField("Long", "SMS_TIMEOUT_SECONDS", "${extra["SMS_TIMEOUT_SECONDS"]}")
        buildConfigField("String", "SMS_SENT_ACTION", "${extra["SMS_SENT_ACTION"]}")
        buildConfigField("int", "DEFAULT_SIM_SLOT", "${extra["DEFAULT_SIM_SLOT"]}")
        buildConfigField("String", "PREFS_NAME", "${extra["PREFS_NAME"]}")
        buildConfigField("String", "KEY_API_KEY", "${extra["KEY_API_KEY"]}")
        buildConfigField("String", "KEY_PORT", "${extra["KEY_PORT"]}")
        buildConfigField("String", "KEY_DEFAULT_SIM_SLOT", "${extra["KEY_DEFAULT_SIM_SLOT"]}")
        buildConfigField("int", "MAX_LOG_LINES", "${extra["MAX_LOG_LINES"]}")
        buildConfigField("String", "CHANNEL_ID", "${extra["CHANNEL_ID"]}")
        buildConfigField("int", "NOTIFICATION_ID", "${extra["NOTIFICATION_ID"]}")
        buildConfigField("String", "ACTION_STOP", "${extra["ACTION_STOP"]}")
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
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.nanohttpd)
    implementation(libs.gson)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}