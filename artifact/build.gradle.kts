plugins {
  id(Plugins.BuildPlugins.androidLib)
  id(Plugins.BuildPlugins.kotlinAndroid)
  id(Plugins.BuildPlugins.mavenPublish)
  jacoco
}

publishArtifact(Releases.Artifact)

createJacocoTestReportTask()

android {
  compileSdk = Sdk.compileSdk

  defaultConfig {
    minSdk = Sdk.minSdk
    targetSdk = Sdk.targetSdk
    testInstrumentationRunner = Dependencies.androidJunitRunner
    // Need to specify this to prevent junit runner from going deep into our dependencies
    testInstrumentationRunnerArguments["package"] = "com.google.android.fhir.artifact"
  }


  // Added this for fixing out of memory issue in running test cases
  tasks.withType<Test>().configureEach {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() - 1).takeIf { it > 0 } ?: 1
    setForkEvery(100)
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
    }
  }

  compileOptions {
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
        "META-INF/*.kotlin_module",
        "readme.html",
      )
    )
  }


  kotlinOptions { jvmTarget = Java.kotlinJvmTarget.toString() }

  configureJacocoTestOptions()
}

configurations {
  all {
    exclude(module = "json")
    exclude(module = "xpp3")
    exclude(module = "hamcrest-all")
    exclude(module = "javax.activation")
    exclude(group = "org.apache.httpcomponents")
    exclude(module = "activation", group = "javax.activation")
    exclude(module = "javaee-api", group = "javax")
    exclude(module = "hamcrest-all")
    exclude(module = "javax.activation")
    exclude(group = "xml-apis")
    exclude(group = "com.google.code.javaparser")
    exclude(group = "jakarta.activation")
  }

}

dependencies {
  androidTestImplementation(Dependencies.AndroidxTest.core)
  androidTestImplementation(Dependencies.AndroidxTest.extJunit)
  androidTestImplementation(Dependencies.AndroidxTest.extJunitKtx)
  androidTestImplementation(Dependencies.AndroidxTest.runner)
  androidTestImplementation(Dependencies.AndroidxTest.workTestingRuntimeKtx)
  androidTestImplementation(Dependencies.junit)
  androidTestImplementation(Dependencies.truth)
  androidTestImplementation(project(":workflow"))
  androidTestImplementation(project(":engine"))
  androidTestImplementation(project(":workflow-testing"))

  api(Dependencies.HapiFhir.structuresR4) { exclude(module = "junit") }

  implementation(Dependencies.Androidx.coreKtx)
  implementation(Dependencies.Cql.translatorModel)
  implementation(Dependencies.Cql.translatorCqlToElm)
  implementation(Dependencies.Cql.translatorElm)
  implementation(Dependencies.Jackson.annotations)
  implementation(Dependencies.Jackson.core)
  implementation(Dependencies.Jackson.databind)
  implementation(Dependencies.Kotlin.kotlinCoroutinesAndroid)
  implementation(Dependencies.Kotlin.kotlinCoroutinesCore)
  implementation(Dependencies.Kotlin.stdlib)
  implementation(Dependencies.woodstox)
  implementation(Dependencies.xerces)

  // HAPI base
  implementation("ca.uhn.hapi.fhir:org.hl7.fhir.convertors:5.4.0")
  implementation("ca.uhn.hapi.fhir:org.hl7.fhir.utilities:5.4.0")

  implementation("ca.uhn.hapi.fhir:hapi-fhir-base:5.4.0")
  implementation("ca.uhn.hapi.fhir:hapi-fhir-converter:5.4.0")
  implementation("ca.uhn.hapi.fhir:hapi-fhir-structures-dstu3:5.4.0")
  implementation(Dependencies.HapiFhir.validation)

  testImplementation(Dependencies.AndroidxTest.core)
  testImplementation(Dependencies.junit)
  testImplementation(Dependencies.robolectric)
  testImplementation(Dependencies.truth)
  testImplementation(project(":engine"))
  testImplementation(project(":workflow"))
  testImplementation(project(":workflow-testing"))
}

configureDokka(Releases.Artifact.artifactId, Releases.Artifact.version)
