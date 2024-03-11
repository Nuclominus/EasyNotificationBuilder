import java.util.Properties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    `maven-publish`
}

apply(from = rootProject.file("publishing.gradle"))

android {
    namespace = "io.github.nuclominus.notifications"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
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

    val keyProps = Properties()
    val keyProperties = File(rootProject.rootDir, "key.properties")
    if (keyProperties.exists() && keyProperties.isFile) {
        keyProperties.inputStream().use { keyProps.load(it) }
    }

    signingConfigs {
        create("release") {
            storeFile = keyProps.getProperty("storeFile")?.let { file(it) }
            storePassword = keyProps.getProperty("storePassword")
            keyAlias = keyProps.getProperty("keyAlias")
            keyPassword = keyProps.getProperty("keyPassword")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
}