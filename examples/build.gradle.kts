plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib"))
    implementation(libs.kotlinx.coroutines.core)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
