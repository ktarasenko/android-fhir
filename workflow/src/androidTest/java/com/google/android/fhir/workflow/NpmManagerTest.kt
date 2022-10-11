package com.google.android.fhir.workflow

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.workflow.testing.CqlBuilder
import com.google.common.truth.Truth.assertThat
import java.io.FileInputStream
import kotlinx.coroutines.runBlocking
import org.cqframework.fhir.npm.NpmPackageManager
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.ImplementationGuide
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.utilities.TextFile
import org.hl7.fhir.utilities.npm.NpmPackage

import org.junit.Test
import org.junit.runner.RunWith
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class NpmManagerTest {

  private val fhirContext = FhirContext.forR4()
  private val jsonParser = fhirContext.newJsonParser()
  private val libraryContentProvider = FhirEngineLibraryContentProvider(AdapterFactory())
  private val loader = NpmPackage.ITransformingLoader {
    runBlocking {
      try {
        println(it)
        val resource = jsonParser.parseResource(FileInputStream(it))
        if (resource is Resource) {
          loadResource(resource)
        }
      } catch (exception: Exception) {
        println(exception)
      }
    }

    TextFile.fileToBytes(it)
  }

  private fun loadResource(resource: Resource) {
    when (resource.resourceType) {
      ResourceType.Library -> {
        fhirOperator.loadLib(resource as Library)
        libraryContentProvider.libs[(resource).name] = resource
      }
      ResourceType.Bundle -> (resource as Bundle).entry.forEach { loadResource(it.resource) }
      else -> runBlocking { fhirEngine.create(resource) }
    }
  }

  private var fhirEngine =
    FhirEngineProvider.getInstance(ApplicationProvider.getApplicationContext())
  private var fhirOperator = FhirOperator(fhirContext, fhirEngine)

  @Test
  fun importCQL() {
    val sourceIg = createFakeIgWithDependencies(
      listOf(Triple("hl7.fhir.us.cqfmeasures", "3.0.0", null), Triple("fhir.cdc.opioid-mme-r4", "3.0.0", "http://fhir.org/guides/cdc/opioid-mme-r4"))
    )

    NpmPackageManager.fromResource(
        ApplicationProvider.getApplicationContext(),
        sourceIg,
        /* version = */  "4.0.1",
        /* ...packageServers = */ "https://packages.fhir.org", "https://packages.simplifier.net",
      ).npmList.forEach { it.loadAllFiles(loader) }


    loadFile("/first-contact/01-registration/patient-charity-otala-1.json")

    val results =
      fhirOperator.evaluateLibrary(
        "http://localhost/Library/SupplementalDataElements|2.0.000",
        "charity-otala-1",
        setOf("SDE Sex")
      ) as Parameters

    assertThat((results.getParameter("SDE Sex") as Coding).display).isEqualTo("Female")


    val cql = """
      library TestLibrary version '1.0.0'
      using FHIR version '4.0.1'
  
      include "SupplementalDataElements" version '2.0.000' called SDE
  
      context Patient

      define "TEST Gender":
        SDE."SDE Sex"
      
    """.trimIndent()

    loadResource(CqlBuilder.compileAndBuild(cql, libraryContentProvider))
    val results1 =
      fhirOperator.evaluateLibrary(
        "http://localhost/Library/TestLibrary|1.0.0",
        "charity-otala-1",
        setOf("TEST Gender")
      ) as Parameters

    assertThat((results1.getParameter("TEST Gender") as Coding).display).isEqualTo("Female")
  }


  private fun loadFile(fileName: String) {
        loadResource(jsonParser.parseResource(open(fileName)) as Resource)
  }

  private fun open(path: String) = NpmManagerTest::class.java.getResourceAsStream(path)!!


  private fun createFakeIgWithDependencies(dependencies: List<Triple<String, String, String?>>) =
    ImplementationGuide().apply {
      dependencies.forEach {
        addDependsOn(ImplementationGuide.ImplementationGuideDependsOnComponent()
                       .apply {
                         packageId = it.first
                         version = it.second
                         uri = it.third
                       })
      }
    }


}
