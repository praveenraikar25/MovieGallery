import com.android.build.api.variant.BuildConfigField
import java.util.Properties

plugins {
    // AGP 9 has built-in Kotlin support — no org.jetbrains.kotlin.android
    // plugin needed, but the Compose compiler plugin is still required.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)
    filter {
        // Hilt/KSP generate a lot of Kotlin under build/ that must not be linted.
        exclude { it.file.path.contains("/build/") }
    }
}

// TMDB v4 read access token, resolved lazily so the configuration cache stays
// valid. providers.fileContents() is a tracked ValueSource — reading the file
// directly at configuration time would defeat the cache. An env var wins over
// local.properties so CI can inject the token, and the empty fallback keeps the
// build configurable when neither is present (requests then fail with a 401).
val tmdbReadAccessToken: Provider<String> =
    providers
        .environmentVariable("TMDB_READ_ACCESS_TOKEN")
        .orElse(
            providers
                .fileContents(layout.settingsDirectory.file("local.properties"))
                .asText
                .map { text ->
                    Properties()
                        .apply { load(text.reader()) }
                        .getProperty("tmdb.readAccessToken")
                        .orEmpty()
                },
        ).orElse("")

android {
    namespace = "com.raikar.moviegallery"
    compileSdk {
        version =
            release(37) {
                minorApiLevel = 1
            }
    }

    defaultConfig {
        applicationId = "com.raikar.moviegallery"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// buildConfigField() on defaultConfig is removed in AGP 10 — the variant API is
// the forward-compatible way to emit this. The String value is written verbatim,
// so it must carry its own quotes.
androidComponents {
    onVariants { variant ->
        // Nullable in AGP 9 — null when the buildConfig feature is off.
        variant.buildConfigFields?.put(
            "TMDB_READ_ACCESS_TOKEN",
            tmdbReadAccessToken.map { token ->
                BuildConfigField("String", "\"$token\"", "TMDB v4 read access token")
            },
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.okhttp.bom))
    testImplementation(libs.okhttp.mockwebserver3.junit4)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
