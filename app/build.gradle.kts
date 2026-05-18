import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
}

android {
  namespace = "com.notdefterim.app"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.notdefterim.app"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "1.0.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    resourceConfigurations += listOf("tr", "en", "ru", "es", "de", "fr")

    // local.properties'tan gizli değerleri oku
    val localPropsFile = rootProject.file("local.properties")
    val localProperties = Properties()
    if (localPropsFile.exists()) {
      localProperties.load(localPropsFile.inputStream())
    }
    val driveWrapPassword: String = localProperties.getProperty(
      "DRIVE_BACKUP_WRAP_PASSWORD"
    ) ?: System.getenv("DRIVE_BACKUP_WRAP_PASSWORD") ?: "__MISSING_SECRET__"
    buildConfigField("String", "DRIVE_BACKUP_WRAP_PASSWORD", "\"$driveWrapPassword\"")
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
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

  kotlinOptions {
    jvmTarget = "17"
  }

  packaging {
    resources {
      // Google API istemcileri çakışan meta-data dosyaları içerebilir
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      excludes += "META-INF/DEPENDENCIES"
      excludes += "META-INF/INDEX.LIST"
    }
  }
}

dependencies {
  // Compose BOM — tek versiyon yönetimi
  val composeBom = platform(libs.compose.bom)
  implementation(composeBom)
  implementation(libs.compose.material3)
  implementation(libs.compose.ui)
  implementation(libs.compose.ui.tooling.preview)
  implementation(libs.compose.material.icons)
  debugImplementation(libs.compose.ui.tooling)

  // Activity + Navigation
  implementation(libs.activity.compose)
  implementation(libs.navigation.compose)
  implementation("androidx.appcompat:appcompat:1.6.1")

  // Room + SQLCipher (AES-256 veritabanı şifreleme)
  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  ksp(libs.room.compiler)
  implementation(libs.sqlcipher)

  // Hilt DI
  implementation(libs.hilt.android)
  ksp(libs.hilt.android.compiler)
  implementation(libs.hilt.navigation.compose)
  implementation(libs.hilt.work)
  ksp(libs.hilt.compiler)

  // Biyometrik kimlik doğrulama
  implementation(libs.biometric)

  // EncryptedSharedPreferences (passphrase saklama)
  implementation(libs.security.crypto)

  // WorkManager — periyodik yedekleme
  implementation(libs.work.runtime.ktx)

  // Lifecycle
  implementation(libs.lifecycle.runtime.compose)
  implementation(libs.lifecycle.viewmodel.compose)
  // ProcessLifecycleOwner — BroadcastReceiver içinde lifecycle-aware coroutine scope için
  implementation(libs.lifecycle.process)

  // Coroutines
  implementation(libs.coroutines.android)
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

  // Serialization (Drive yedek paketi için JSON)
  implementation(libs.kotlinx.serialization.json)

  // Google Sign-In & Drive REST API
  implementation(libs.play.services.auth)
  implementation(libs.google.api.client.android)
  implementation(libs.google.api.services.drive)
  implementation(libs.google.http.client.gson)

  // Test
  testImplementation(libs.junit)
  androidTestImplementation(libs.junit.ext)
}
