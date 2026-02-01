import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun envOrProp(envKey: String, propKey: String): String? =
    System.getenv(envKey) ?: keystoreProperties.getProperty(propKey)

val releaseStoreFile = envOrProp("RELEASE_STORE_FILE", "storeFile")
val releaseStorePassword = envOrProp("RELEASE_KEYSTORE_PASSWORD", "storePassword")
val releaseKeyAlias = envOrProp("RELEASE_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = envOrProp("RELEASE_KEY_PASSWORD", "keyPassword")

android {
    namespace = "dev.nutting.kexplore"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.nutting.kexplore"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { !it.isNullOrBlank() }) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release") {
            signingConfig = try {
                signingConfigs.getByName("release")
            } catch (_: UnknownDomainObjectException) {
                if (System.getenv("CI") != null) {
                    throw GradleException("Release signing config missing in CI. Check repository secrets.")
                }
                signingConfigs.getByName("debug")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")

    debugImplementation("androidx.compose.ui:ui-tooling")

    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
