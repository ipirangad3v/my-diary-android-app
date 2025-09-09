import java.io.FileInputStream
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.services)
  alias(libs.plugins.hilt.android)
  alias(libs.plugins.kotlin.ksp)
  alias(libs.plugins.spotless)
  alias(libs.plugins.jacoco)
}

android {
  namespace = "digital.tonima.mydiary"
  compileSdk = 36

  defaultConfig {
    applicationId = "digital.tonima.mydiary"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    debug {
      val admobAppIdTest = "ca-app-pub-3940256099942544~3347511713"
      val admobBannerAdUnitIdTest = "ca-app-pub-3940256099942544/6300978111"

      resValue("string", "admob_app_id", admobAppIdTest)
      buildConfigField("String", "ADMOB_BANNER_AD_UNIT_HOME", "\"$admobBannerAdUnitIdTest\"")
      buildConfigField(
          "String", "ADMOB_BANNER_AD_UNIT_LOCKED_DIARY", "\"$admobBannerAdUnitIdTest\"")
    }
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("debug")

      val localProperties = Properties()
      val localPropertiesFile = rootProject.file("local.properties")
      if (localPropertiesFile.exists()) {
        localProperties.load(FileInputStream(localPropertiesFile))
      }

      val admobAppId = System.getenv("ADMOB_APP_ID") ?: localProperties.getProperty("admob.app.id")

      val admobBannerAdUnitIdHome =
          System.getenv("ADMOB_BANNER_AD_UNIT_HOME")
              ?: localProperties.getProperty("admob.banner.ad.unit.home")

      val admobBannerAdUnitIdLockedDiary =
          System.getenv("ADMOB_BANNER_AD_UNIT_LOCKED_DIARY")
              ?: localProperties.getProperty("admob.banner.ad.unit.locked.diary")

      resValue("string", "admob_app_id", admobAppId)
      buildConfigField("String", "ADMOB_BANNER_AD_UNIT_HOME", "\"$admobBannerAdUnitIdHome\"")
      buildConfigField(
          "String", "ADMOB_BANNER_AD_UNIT_LOCKED_DIARY", "\"$admobBannerAdUnitIdLockedDiary\"")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  kotlin { jvmToolchain(21) }
  buildFeatures {
    compose = true
    buildConfig = true
  }
}

tasks.register<JacocoReport>("createDebugCoverageReport") {
  dependsOn("testDebugUnitTest")

  reports {
    xml.required.set(true)
    html.required.set(true)
  }

  val fileFilter =
      listOf(
          // Android
          "**/R.class",
          "**/R$*.class",
          "**/BuildConfig.*",
          "**/Manifest*.*",
          "**/*Test*.*",
          "android/**/*.*",
          // DI, generated code
          "**/*_Hilt*.class",
          "**/Dagger*Component.class",
          "**/Dagger*Module.class",
          "**/Dagger*Module_Provide*Factory.class",
          "**/*_Provide*Factory*.*",
          "**/*_Factory*.*")

  val debugTree =
      fileTree("${layout.buildDirectory}/tmp/kotlin-classes/debug") { exclude(fileFilter) }
  val mainSrc = "${project.projectDir}/src/main/java"

  sourceDirectories.setFrom(files(mainSrc))
  classDirectories.setFrom(files(debugTree))
  executionData.setFrom(
      fileTree(layout.buildDirectory) {
        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
      })
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.google.firebase.bom))
  implementation(libs.google.firebase.analytics)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.biometrics)
  implementation(libs.androidx.fragment.ktx)
  implementation(libs.androidx.security.crypto)
  implementation(libs.compose.calendar)
  implementation(libs.hilt.android)
  implementation(libs.core.ktx)
  implementation(libs.play.services.ads.api)
  ksp(libs.hilt.compiler)
  implementation(libs.hilt.binder)
  ksp(libs.hilt.binder.compiler)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.mockk)
  testImplementation(libs.turbine)
  testImplementation(libs.truth)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.core.testing)

  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)

  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}

apply(from = "../spotless.gradle")
