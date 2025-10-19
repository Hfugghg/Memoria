plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.exp.memoria"
    compileSdk = 34 // 注意：这里用的是 '=' 赋值，并且版本号是有效的 34

    defaultConfig {
        applicationId = "com.exp.memoria"
        minSdk = 24
        targetSdk = 34 // 注意：这里也改成了有效的版本号 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // 使用 findProperty 更安全，它不会在属性不存在时抛出异常，而是返回 null
            val keystoreFileProp = project.findProperty("SIGNING_KEYSTORE_FILE")

            if (keystoreFileProp != null) {
                // 确保所有必需的属性都存在
                val storePass = project.findProperty("SIGNING_KEYSTORE_PASSWORD")?.toString()
                val keyAliasName = project.findProperty("SIGNING_KEY_ALIAS")?.toString()
                val keyPass = project.findProperty("SIGNING_KEY_PASSWORD")?.toString()

                // 只有当所有属性都存在时才配置签名
                if (storePass != null && keyAliasName != null && keyPass != null) {
                    storeFile = file(keystoreFileProp.toString())
                    storePassword = storePass
                    keyAlias = keyAliasName
                    keyPassword = keyPass
                } else {
                    println("Warning: One or more signing secrets are missing, release signing config will be incomplete.")
                }
            } else {
                // 这将在本地开发环境中，如果属性未设置，使用默认/调试签名
                println("Info: SIGNING_KEYSTORE_FILE property not found. Ensure this is intentional for your current environment.")
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
            signingConfig = signingConfigs.getByName("release")
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
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8" // 这个版本匹配 Kotlin 1.9.22
    }

    packaging { // 这一段最好也加上，可以避免一些常见的打包问题
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    //testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Hilt (依赖注入)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room (数据库)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-common:2.6.1") // 为解决FTS5注解找不到的问题，显式添加此依赖
    ksp("androidx.room:room-compiler:2.6.1") // 使用 ksp 替代 kapt 来处理 Room
    implementation("androidx.room:room-ktx:2.6.1")

    // 用于 Compose 的 ViewModel 和 LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // WorkManager (用于后台任务)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Retrofit (网络请求)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // OkHttp - Retrofit 的底层依赖，解决 "Unresolved reference 'okhttp3'" 问题
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Kotlinx Serialization 核心库
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Retrofit 的 Kotlinx Serialization 转换器
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.0-beta01")
    implementation("androidx.datastore:datastore-core:1.1.0-beta01")
}
