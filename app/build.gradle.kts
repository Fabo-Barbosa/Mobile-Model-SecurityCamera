import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // ... outras plugins
    id("com.google.dagger.hilt.android") version("2.51.1")
    kotlin("kapt") // Para o Kapt, que Hilt usa para gerar código
}

// Carregar propriedades do local.properties
val properties = Properties()
if (rootProject.file("local.properties").exists()) {
    rootProject.file("local.properties").inputStream().use { properties.load(it) }
}

android {
    namespace = "br.com.fabolearn.ezvizshowcamera"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.com.fabolearn.ezvizshowcamera"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
        }

    }

    buildTypes {
        debug {
            buildConfigField("String", "APP_KEY", "\"${properties.getProperty("APP_KEY", "")}\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Expor as chaves como BuildConfig fields
            buildConfigField("String", "APP_KEY", "\"${properties.getProperty("APP_KEY", "")}\"")
            buildConfigField("String", "AUTH_TOKEN", "\"${properties.getProperty("AUTH_TOKEN", "")}\"")
            buildConfigField("String", "SECRET", "\"${properties.getProperty("SECRET", "")}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
android.buildFeatures.buildConfig = true
    buildFeatures {
        viewBinding = true
    }

    hilt {
        enableAggregatingTask = false
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.foundation.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Android KTX
    implementation("androidx.core:core-ktx:1.13.1") // Verifique a versão mais recente

    // AppCompat
    implementation("androidx.appcompat:appcompat:1.7.0") // Verifique a versão mais recente

    // Material Design (para AppBar, Botões, TextViews, etc.)
    implementation("com.google.android.material:material:1.12.0") // Verifique a versão mais recente

    // ConstraintLayout (opcional, mas comum para layouts complexos)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2") // Verifique a versão mais recente

    // Lifecycle (ViewModel, LiveData, lifecycleScope)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0") // Para lifecycleScope.launch

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // ... outras dependências
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")

    /* EZVIZ SDK core module, must rely on */
    implementation("io.github.ezviz-open:ezviz-sdk:5.13")
    //After version 4.19.0, you need to rely on okhttp and gson libraries
    implementation("com.squareup.okhttp3:okhttp:3.12.1")
    implementation("com.google.code.gson:gson:2.8.5")

    /* Video calls module, use if needed */
    implementation("io.github.ezviz-open:videotalk:1.3.0")

    /* Code stream acquisition module, use if needed */
    implementation("io.github.ezviz-open:streamctrl:1.3.0")

    /* Dependência para leitura de QR Code*/
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.3")

    /* Preferências compartilhadas, dependência para persistir o tema */
    implementation("androidx.preference:preference-ktx:1.2.1")
}