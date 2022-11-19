plugins {
  id(Plugins.BuildPlugins.androidLib)
  id(Plugins.BuildPlugins.kotlinAndroid)
  id(Plugins.BuildPlugins.mavenPublish)
  jacoco
}

publishArtifact(Releases.ImplmentationGuide)

createJacocoTestReportTask()

android {
  compileSdk = Sdk.compileSdk

  defaultConfig {
    minSdk = Sdk.minSdk
    targetSdk = Sdk.targetSdk
    testInstrumentationRunner = Dependencies.androidJunitRunner
    // Need to specify this to prevent junit runner from going deep into our dependencies
    testInstrumentationRunnerArguments["package"] = "com.google.android.fhir.implementationguide"
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = Java.sourceCompatibility
    targetCompatibility = Java.targetCompatibility
  }

  packagingOptions {
    resources.excludes.addAll(
      listOf(
        "license.html",
        "META-INF/ASL2.0",
        "META-INF/ASL-2.0.txt",
        "META-INF/DEPENDENCIES",
        "META-INF/LGPL-3.0.txt",
        "META-INF/LICENSE",
        "META-INF/LICENSE.txt",
        "META-INF/license.txt",
        "META-INF/license.html",
        "META-INF/LICENSE.md",
        "META-INF/NOTICE",
        "META-INF/NOTICE.txt",
        "META-INF/NOTICE.md",
        "META-INF/notice.txt",
        "META-INF/LGPL-3.0.txt",
        "META-INF/sun-jaxb.episode",
      )
    )
  }

  kotlinOptions { jvmTarget = Java.kotlinJvmTarget.toString() }

  configureJacocoTestOptions()
}

configurations {
  all {
    exclude(module = "xpp3")
    exclude(module = "xpp3_min")
  }
}

dependencies {
  coreLibraryDesugaring(Dependencies.desugarJdkLibs)
  kapt(Dependencies.Room.compiler)

  androidTestImplementation(Dependencies.AndroidxTest.core)
  androidTestImplementation(Dependencies.AndroidxTest.runner)
  androidTestImplementation(Dependencies.AndroidxTest.extJunitKtx)
  androidTestImplementation(Dependencies.Kotlin.kotlinCoroutinesTest)
  androidTestImplementation(Dependencies.junit)
  androidTestImplementation(Dependencies.truth)

  implementation(Dependencies.Kotlin.stdlib)
  implementation(Dependencies.Lifecycle.liveDataKtx)
  implementation(Dependencies.Room.ktx)
  implementation(Dependencies.Room.runtime)
  implementation(Dependencies.timber)
  implementation(Dependencies.Cql.evaluatorFhir)
  implementation(Dependencies.Cql.engine)

  api(Dependencies.HapiFhir.structuresR4) { exclude(module = "junit") }

  testImplementation(Dependencies.AndroidxTest.archCore)
  testImplementation(Dependencies.AndroidxTest.core)
  testImplementation(Dependencies.Kotlin.kotlinCoroutinesTest)
  testImplementation(Dependencies.junit)
  testImplementation(Dependencies.mockitoInline)
  testImplementation(Dependencies.mockitoKotlin)
  testImplementation(Dependencies.robolectric)
  testImplementation(Dependencies.truth)

  // remove me
  implementation("org.apache.commons:commons-compress:1.20")
  implementation("com.google.code.gson:gson:2.10")
  implementation("ca.uhn.hapi.fhir:org.hl7.fhir.convertors:5.6.36")
  androidTestImplementation("org.opencds.cqf.cql:evaluator.measure-hapi:2.1.0")
  testImplementation("org.opencds.cqf.cql:evaluator.measure-hapi:2.1.0")
  implementation(Dependencies.Cql.engineJackson) // Necessary to import Executable XML/JSON CQL libs
  implementation(
    Dependencies.Cql.translatorModelJackson
  ) // Necessary to import Executable XML/JSON CQL libs
}

configureDokka(Releases.ImplmentationGuide.artifactId, Releases.ImplmentationGuide.version)
