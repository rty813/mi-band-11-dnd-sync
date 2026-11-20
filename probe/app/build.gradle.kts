buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
    }
}

plugins {
    id("com.android.application") version "9.0.1"
}

android {
    namespace = "com.example.banddndprobe"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.banddndprobe"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
}
