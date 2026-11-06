import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // AGP 9 does not allow the KMP plugin on an application module, so the code both
    // platforms share lives in this library; :app stays the Android application.
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
    alias(libs.plugins.apollo)
    alias(libs.plugins.ktlint)
}

kotlin {
    android {
        namespace = "com.raikar.moviegallery.shared"
        compileSdk = 37
        minSdk = 29

        // Matches :app's compileOptions; a newer target here breaks inlining into :app.
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        // Runs commonTest (and androidHostTest) on the JVM as part of ./gradlew test.
        withHostTest {}
    }
    iosArm64()
    iosSimulatorArm64()

    // Room's multiplatform database constructor is an expect object, which Kotlin still
    // flags as Beta on every compile.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
            api(libs.apollo.runtime)
            api(libs.room3.runtime)
            api(libs.ktor.client.core)
            // api: NetworkConfig and the client factories expose LogLevel and Logger.
            api(libs.ktor.client.logging)
            api(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// Exported schemas are committed so each future migration can be checked against
// the version it upgrades from.
room3 {
    schemaDirectory("$projectDir/schemas")
}

// Room's compiler runs once per target; each generates that target's database constructor.
dependencies {
    add("kspAndroid", libs.room3.compiler)
    add("kspIosArm64", libs.room3.compiler)
    add("kspIosSimulatorArm64", libs.room3.compiler)
}

// The Countries GraphQL API backs the production-country details on Movie Detail. The schema is
// committed so builds never need the network; refresh it with
// ./gradlew :shared:downloadCountriesApolloSchemaFromIntrospection
apollo {
    service("countries") {
        packageName.set("com.raikar.moviegallery.data.remote.graphql.countries")
        srcDir("src/commonMain/graphql/countries")
        introspection {
            endpointUrl.set("https://countries.trevorblades.com/graphql")
            schemaFile.set(file("src/commonMain/graphql/countries/schema.graphqls"))
        }
    }
}

// KMP modules have no `test` task, so a root ./gradlew test would silently skip this module.
// This one runs the JVM (Android host) tests; iOS runs separately via iosSimulatorArm64Test.
tasks.register("test") {
    group = "verification"
    description = "Runs the shared module's Android host tests (commonTest + androidHostTest)."
    dependsOn("testAndroidHostTest")
}

ktlint {
    ignoreFailures.set(false)
    filter {
        // KSP and Apollo generate Kotlin under build/ that must not be linted.
        exclude { it.file.path.contains("/build/") }
    }
}
