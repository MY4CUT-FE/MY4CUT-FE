plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    // id("com.google.gms.google-services") // 파이어베이스 사용 시 주석 해제
}

android {
    namespace = "com.umc.mobile.my4cut"
    compileSdk = 34 // 36은 아직 불안정할 수 있어 34(Android 14) 권장, 필요시 36 유지 가능

    defaultConfig {
        applicationId = "com.umc.mobile.my4cut"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8 // 또는 VERSION_17 권장
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "1.8" // 또는 "17"
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1" // 코틀린 버전에 맞춰 수정 필요할 수 있음
    }
}

dependencies {
    // ==========================================
    // 1. Android Core & Kotlin (기본)
    // ==========================================
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // 자바 최신 기능 지원 (Desugaring)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // ==========================================
    // 2. UI - Jetpack Compose (최신 방식)
    // ==========================================
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ==========================================
    // 3. Navigation (화면 이동)
    // ==========================================
    val navVersion = "2.7.7"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")
    implementation("androidx.navigation:navigation-compose:$navVersion")

    // ==========================================
    // 4. Network (서버 통신)
    // ==========================================
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.9.0")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // GSON
    implementation("com.google.code.gson:gson:2.10.1")

    // ==========================================
    // 5. Image Loading (Glide)
    // ==========================================
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // ==========================================
    // 6. Local Database (Room)
    // ==========================================
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // ==========================================
    // 7. Third Party Libraries (기타)
    // ==========================================
    // Kakao Login
    implementation("com.kakao.sdk:v2-user:2.19.0") // 필요한 모듈만 추가

    // Custom UI
    implementation("com.kizitonwose.calendar:view:2.5.0") // CalendarView
    implementation("de.hdodenhof:circleimageview:3.1.0") // 원형 이미지
    implementation("me.relex:circleindicator:2.1.6") // 인디케이터

    // ==========================================
    // 8. Testing (테스트)
    // ==========================================
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}