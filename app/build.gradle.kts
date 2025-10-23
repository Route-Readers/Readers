import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.route.readers"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.route.readers"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // local.properties 파일 로드
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }

        // ALADIN_TTB_KEY를 BuildConfig.ALADIN_TTB_KEY로 생성
        buildConfigField(
            "String",
            "ALADIN_TTB_KEY",
            "\"${localProperties.getProperty("ALADIN_TTB_KEY")}\""
        )

        buildConfigField(
            "String",
            "DATA_GO_KR_API_KEY", // <- 이 이름을 Kotlin 코드에서 사용하는 이름과 일치시킵니다.
            "\"${localProperties.getProperty("DATA_GO_KR_API_KEY")}\""
        )


    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true // 이 설정이 있어야 BuildConfig 파일이 생성됩니다.
    }

}

dependencies {
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
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("androidx.compose.foundation:foundation-layout:1.6.8")

    // --- Firebase ---
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // --- Retrofit ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")

    // --- Coil for image loading ---
    implementation("io.coil-kt:coil-compose:2.4.0")
    // 위치
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // HTTP 요청/응답을 로그로 보기 위한 라이브러리
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

}
