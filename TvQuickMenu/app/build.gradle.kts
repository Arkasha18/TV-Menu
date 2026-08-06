plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

import java.util.Properties

val signingProps = Properties().apply {
    val localFile = rootProject.file("../keys/signing.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}

fun signingProp(name: String): String? =
    providers.gradleProperty(name).orNull
        ?: providers.environmentVariable(name).orNull
        ?: signingProps.getProperty(name)?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingProp("TV_MENU_STORE_FILE")
val releaseStorePassword = signingProp("TV_MENU_STORE_PASSWORD")
val releaseKeyAlias = signingProp("TV_MENU_KEY_ALIAS")
val releaseKeyPassword = signingProp("TV_MENU_KEY_PASSWORD")
val releaseStoreType = signingProp("TV_MENU_STORE_TYPE") ?: "PKCS12"
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.h9.tvquickmenu"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.h9.tvquickmenu"
        minSdk = 28
        targetSdk = 34
        versionCode = 3
        versionName = "1.2.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("releaseKey") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                storeType = releaseStoreType
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("releaseKey")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = false
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.md",
                "META-INF/NOTICE.txt",
                "META-INF/DEPENDENCIES"
            )
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.2")
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("dev.mobile:dadb:2.0.0") {
        exclude(group = "org.junit.jupiter")
        exclude(group = "org.junit.platform")
        exclude(group = "org.junit.vintage")
        exclude(group = "junit")
    }
}
