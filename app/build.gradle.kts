plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release APKs come out unsigned and are signed by hand with the Numio key,
// so the key never leaves RR's own computer.
android {
    namespace = "com.rr.numio.launcher"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.rr.numio.launcher"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
        }
        // Test builds from GitHub: same as release (R8 on), but signed with a throwaway
        // test key and installed next to the real app as "Launcher Test".
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".test"
            versionNameSuffix = "-test"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Dock and home settings
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
