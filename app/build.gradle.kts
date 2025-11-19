import java.util.Properties
import java.io.FileInputStream

plugins {id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.route.readers"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.route.readers"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }

        buildConfigField(
            "String",
            "ALADIN_TTB_KEY",
            "\"${localProperties.getProperty("ALADIN_TTB_KEY")}\""
        )

        buildConfigField(
            "String",
            "DATA_GO_KR_API_KEY",
            "\"${localProperties.getProperty("DATA_GO_KR_API_KEY")}\""
        )
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-ads:23.2.0")
    implementation("com.google.android.material:material:1.13.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.core:core-splashscreen:1.2.0")

    implementation("androidx.compose.foundation:foundation-layout:1.6.8")

    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")


    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")

    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1") //다크모드
    implementation("co.yml:ycharts:2.1.0")
    implementation("androidx.lifecycle:lifecycle-process:2.8.4")



    val calendarVersion = "1.4.0"
    implementation("io.github.boguszpawlowski.composecalendar:composecalendar:$calendarVersion")
    implementation("io.github.boguszpawlowski.composecalendar:kotlinx-datetime:$calendarVersion")
}
