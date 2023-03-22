/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.fhir.workflow

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.implementationguide.IgManager
import com.google.android.fhir.testing.FhirEngineProviderTestRule
import com.google.android.fhir.workflow.testing.CqlBuilder
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.InputStream
import java.util.TimeZone
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.ActivityDefinition
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.Measure
import org.hl7.fhir.r4.model.MetadataResource
import org.hl7.fhir.r4.model.PlanDefinition
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.opencds.cqf.cql.evaluator.measure.common.MeasureEvalType
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert.assertEquals

@RunWith(RobolectricTestRunner::class)
class FhirOperatorTest {
  @get:Rule val fhirEngineProviderRule = FhirEngineProviderTestRule()

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val igManager = IgManager.createInMemory(context)
  private val fhirContext = FhirContext.forR4()
  private val jsonParser = fhirContext.newJsonParser()
  private val xmlParser = fhirContext.newXmlParser()

  private lateinit var fhirEngine: FhirEngine
  private lateinit var fhirOperator: FhirOperator

  @Before
  fun setUp() = runBlockingOnWorkerThread {
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
    fhirEngine = FhirEngineProvider.getInstance(context)
    fhirOperator = FhirOperator(fhirContext, fhirEngine, igManager)
    val rootDirectory = File(javaClass.getResource("/anc-cds")!!.file)
    igManager.install(IgManager.DEFAULT_DEPENDENCY, rootDirectory)
  }

  @After
  fun tearDown() {
    igManager.close()
  }

  @Test
  fun generateCarePlan() = runBlockingOnWorkerThread {
    loadBundle(parseJson("/plan-definition/rule-filters/RuleFilters-1.0.0-bundle.json"))
    loadBundle(parseJson("/plan-definition/rule-filters/tests-Reportable-bundle.json"))
    loadBundle(parseJson("/plan-definition/rule-filters/tests-NotReportable-bundle.json"))

    loadFile("/first-contact/01-registration/patient-charity-otala-1.json")
    loadFile("/first-contact/02-enrollment/careplan-charity-otala-1-pregnancy-plan.xml")
    loadFile("/first-contact/02-enrollment/episodeofcare-charity-otala-1-pregnancy-episode.xml")
    loadFile("/first-contact/03-contact/encounter-anc-encounter-charity-otala-1.xml")

    assertThat(
        fhirOperator.generateCarePlan(
          planDefinitionId = "plandefinition-RuleFilters-1.0.0",
          patientId = "Reportable",
          encounterId = "reportable-encounter"
        )
      )
      .isNotNull()
  }

  @Test
  fun generateCarePlanWithoutEncounter() = runBlockingOnWorkerThread {
    loadBundle(parseJson("/plan-definition/med-request/med_request_patient.json"))
    loadBundle(parseJson("/plan-definition/med-request/med_request_plan_definition.json"))

    val carePlan =
      fhirOperator.generateCarePlan(
        planDefinitionId = "MedRequest-Example",
        patientId = "Patient/Patient-Example"
      )

    println(jsonParser.encodeResourceToString(carePlan))

    assertEquals(
      readResourceAsString("/plan-definition/med-request/med_request_careplan.json"),
      jsonParser.encodeResourceToString(carePlan),
      true
    )
  }

  @Test
  fun evaluatePopulationMeasure() = runBlockingOnWorkerThread {
    loadFile("/first-contact/01-registration/patient-charity-otala-1.json")
    loadFile("/first-contact/02-enrollment/careplan-charity-otala-1-pregnancy-plan.xml")
    loadFile("/first-contact/02-enrollment/episodeofcare-charity-otala-1-pregnancy-episode.xml")
    loadFile("/first-contact/03-contact/encounter-anc-encounter-charity-otala-1.xml")

    val measureReport =
      fhirOperator.evaluateMeasure(
        measureUrl = "http://fhir.org/guides/who/anc-cds/Measure/ANCIND01",
        start = "2019-01-01",
        end = "2021-12-31",
        reportType = MeasureEvalType.POPULATION.toCode(),
        subject = null,
        practitioner = null
      )

    measureReport.date = null

    assertEquals(
      readResourceAsString("/first-contact/04-results/population-report.json"),
      jsonParser.setPrettyPrint(true).encodeResourceToString(measureReport),
      true
    )
  }

  @Test
  fun evaluateGroupPopulationMeasure() = runBlockingOnWorkerThread {
    val resourceBundle =
      Bundle().apply {
        addEntry().apply {
          resource = toFhirLibrary(open("/group-measure/PatientGroups-1.0.0.cql"))
        }
        addEntry().apply {
          resource = parseResource<Measure>("/group-measure/PatientGroupsMeasure.json")
        }
      }

    loadBundle(resourceBundle)
    loadBundle(parseJson("/group-measure/Data-Patients-bundle.json"))
    loadBundle(parseJson("/group-measure/Data-Groups-bundle.json"))

    val measureReport =
      fhirOperator.evaluateMeasure(
        measureUrl = "Measure/PatientGroupsMeasure",
        start = "2019-01-01",
        end = "2022-12-31",
        reportType = MeasureEvalType.POPULATION.toCode(),
        subject = null,
        practitioner = null
      )

    measureReport.date = null

    assertEquals(
      readResourceAsString("/group-measure/Results-Measure-report.json"),
      jsonParser.setPrettyPrint(true).encodeResourceToString(measureReport),
      true
    )
  }

  @Test
  fun evaluateIndividualSubjectMeasure() = runBlockingOnWorkerThread {
    loadFile("/first-contact/01-registration/patient-charity-otala-1.json")
    loadFile("/first-contact/02-enrollment/careplan-charity-otala-1-pregnancy-plan.xml")
    loadFile("/first-contact/02-enrollment/episodeofcare-charity-otala-1-pregnancy-episode.xml")
    loadFile("/first-contact/03-contact/encounter-anc-encounter-charity-otala-1.xml")
    val measureReport =
      fhirOperator.evaluateMeasure(
        measureUrl = "http://fhir.org/guides/who/anc-cds/Measure/ANCIND01",
        start = "2020-01-01",
        end = "2020-01-31",
        reportType = MeasureEvalType.SUBJECT.toCode(),
        subject = "charity-otala-1",
        practitioner = "jane"
      )

    measureReport.date = null

    assertEquals(
      readResourceAsString("/first-contact/04-results/subject-report.json"),
      jsonParser.setPrettyPrint(true).encodeResourceToString(measureReport),
      true
    )
  }

  private suspend fun loadFile(path: String) {
    if (path.endsWith(suffix = ".xml")) {
      val resource = xmlParser.parseResource(open(path)) as Resource
      fhirEngine.create(resource)
    } else if (path.endsWith(".json")) {
      val resource = jsonParser.parseResource(open(path)) as Resource
      fhirEngine.create(resource)
    }
  }

  private suspend fun loadBundle(bundle: Bundle) {
    for (entry in bundle.entry) {
      val resource = entry.resource
      when (resource.resourceType) {
        ResourceType.Library -> igManager.install(writeToFile(entry.resource as Library))
        ResourceType.Measure -> igManager.install(writeToFile(entry.resource as Measure))
        ResourceType.ActivityDefinition ->
          igManager.install(writeToFile(entry.resource as ActivityDefinition))
        ResourceType.PlanDefinition ->
          igManager.install(writeToFile(entry.resource as PlanDefinition))
        ResourceType.Bundle -> Unit
        else -> fhirEngine.create(entry.resource)
      }
    }
  }

  private fun writeToFile(resource: MetadataResource): File {
    return File(context.filesDir, resource.name ?: resource.idElement.idPart).apply {
      writeText(jsonParser.encodeResourceToString(resource))
    }
  }

  private fun toFhirLibrary(cql: InputStream): Library {
    return CqlBuilder.compileAndBuild(cql)
  }

  private fun open(path: String) = javaClass.getResourceAsStream(path)!!

  private fun parseJson(path: String): Bundle = jsonParser.parseResource(open(path)) as Bundle

  private fun readResourceAsString(path: String) = open(path).readBytes().decodeToString()

  private fun <T : IBaseResource> parseResource(path: String) =
    jsonParser.parseResource(readResourceAsString(path)) as T
}
