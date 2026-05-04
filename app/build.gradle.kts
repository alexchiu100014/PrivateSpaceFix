plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.privatespacefix"
    compileSdk = 35

    defaultConfig {
        minSdk = 35  // Private Space is API 35+ (Android 15 / Vanilla Ice Cream)

        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // 讓 assets/ 目錄中的 xposed_init 被正確打包
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
        }
    }
}

dependencies {
    // libxposed API — compile-only，執行時由 LSPosed framework 提供
    compileOnly("io.github.libxposed:api:101.0.1")

    // AndroidX annotation（@RequiresApi 等）
    compileOnly("androidx.annotation:annotation:1.7.1")
}
