// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // AGP 9 has built-in Kotlin support — no org.jetbrains.kotlin.android
    // plugin needed, but the Compose compiler plugin is still required.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
}

// Points git at the committed .githooks directory so the pre-commit ktlint
// gate is active. Run once per clone: ./gradlew installGitHooks
tasks.register<Exec>("installGitHooks") {
    group = "git hooks"
    description = "Sets core.hooksPath to .githooks so the pre-commit ktlint hook runs."
    workingDir = rootDir
    commandLine("git", "config", "core.hooksPath", ".githooks")
}