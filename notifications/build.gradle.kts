plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.android.kotlin)
    alias(libs.plugins.detekt.analyzer)
    `maven-publish`
    signing
}

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

detekt {
    source.setFrom("src/main/kotlin")
    config.setFrom("../config/detekt/detekt.yaml")
    buildUponDefaultConfig = true
}

project.ext["signing.keyId"] = System.getenv("SIGN_KEY_ID")
project.ext["signing.secretKeyRingFile"] = System.getenv("SIGN_KEY")
project.ext["signing.password"] = System.getenv("SIGN_KEY_PASS")

val sourceJar by tasks.registering(Jar::class) {
    from(android.sourceSets["main"].java.srcDirs)
    archiveClassifier.set("sourcesJar")
}

afterEvaluate {

    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifact(sourceJar)

                groupId = "io.github.nuclominus"
                artifactId = "easynotificationbuilder"
                version = "0.2.1"

                pom {
                    name.set("Easy Notification Builder")
                    description.set("Simple builder for push notifications")
                    url.set("https://github.com/Nuclominus/easynotificationbuilder")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("nuclominus")
                            name.set("Roman Kosko")
                            email.set("9DGRoman@gmail.com")
                        }
                    }

                    scm {
                        url.set("https://github.com/Nuclominus/easynotificationbuilder")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "sonatypeStaging"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = System.getenv("OSS_USERNAME")
                    password = System.getenv("OSS_PASSWORD")
                }
            }
        }
    }

    signing {
        sign(publishing.publications)
    }
}