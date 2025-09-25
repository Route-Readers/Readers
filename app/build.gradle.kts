plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") // dev 브랜치의 Firebase 플러그인 추가
}

android {
    namespace = "com.route.readers"
    compileSdk = 35 // dev 브랜치의 상향된 SDK 버전 적용

    defaultConfig {
        applicationId = "com.route.readers"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    // 기존 공통 의존성
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3") // bom이 버전을 관리하므로 이 줄만 남김
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.runtime:runtime-livedata:1.5.4")

    // --- dev 브랜치에서 추가된 의존성들 ---
    implementation("androidx.compose.material:material-icons-extended-android:1.6.5")
    debugImplementation("androidx.compose.ui:ui-tooling:1.9.1") // androidx.compose.ui:ui-tooling 의존성 이름 통일
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
}