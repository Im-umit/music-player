plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.google.devtools.ksp)
}

android {
  namespace = "com.neonmusic.player"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.neonmusic.player"
    minSdk = 24
    targetSdk = 36
    versionCode = 2
    versionName = "1.0.1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    val keystorePath = System.getenv("KEYSTORE_PATH")
    val storePasswordValue = System.getenv("STORE_PASSWORD")
    val keyPasswordValue = System.getenv("KEY_PASSWORD")
    if (!keystorePath.isNullOrBlank() && !storePasswordValue.isNullOrBlank() &&
      !keyPasswordValue.isNullOrBlank() && file(keystorePath).exists()) {
      create("release") {
        storeFile = file(keystorePath)
        storePassword = storePasswordValue
        keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
        keyPassword = keyPasswordValue
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfigs.findByName("release")?.let { signingConfig = it }
    }
    debug { }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  // implementation(libs.accompanist.permissions)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity)
  // implementation(libs.androidx.datastore.preferences)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.session)
  implementation(libs.androidx.media3.common)
  implementation(libs.androidx.documentfile)
  implementation(libs.androidx.webkit)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.play.services.location)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  "ksp"(libs.androidx.room.compiler)
}
