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
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.implementationguide.IgContext
import com.google.android.fhir.implementationguide.IgManager
import com.google.android.fhir.implementationguide.data.IgManagerFhirDal
import com.google.android.fhir.implementationguide.data.IgManagerLibraryContentProvider
import com.google.android.fhir.implementationguide.data.IgManagerRetrieveProvider
import com.google.android.fhir.implementationguide.data.IgManagerTerminologyProvider
import com.google.android.fhir.implementationguide.db.impl.ImplementationGuideDatabase
import com.google.android.fhir.testing.FhirEngineProviderTestRule
import java.io.InputStream
import java.io.InputStreamReader
import java.util.TimeZone
import kotlinx.coroutines.runBlocking
import org.cqframework.cql.cql2elm.LibrarySourceProvider
import org.hl7.fhir.r4.model.Resource
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal
import org.opencds.cqf.cql.evaluator.measure.common.MeasureEvalType
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

@RunWith(RobolectricTestRunner::class)
class WorkflowIgManagerCrossoverTest {
  @get:Rule val fhirEngineProviderRule = FhirEngineProviderTestRule()

  private lateinit var fhirEngine: FhirEngine
  private lateinit var fhirOperator: FhirOperator
  private lateinit var fhirEngineApiProvider: FhirEngineApiProvider
  private lateinit var igApiProvider: WorkflowApiProvider
  private lateinit var igManager: IgManager
  private lateinit var igDependency: IgContext.ImplementationGuideDependency

  companion object {
    private val fhirContext = FhirContext.forR4()
    private val jsonParser = fhirContext.newJsonParser()
    private val xmlParser = fhirContext.newXmlParser()

    private fun open(path: String) = FhirOperatorTest::class.java.getResourceAsStream(path)!!

    private fun readResourceAsString(path: String) = open(path).readBytes().decodeToString()
  }

  @Before
  fun setUp() = runBlocking {
    fhirEngine = FhirEngineProvider.getInstance(ApplicationProvider.getApplicationContext())
    fhirEngineApiProvider = FhirEngineApiProvider(fhirContext, fhirEngine)
    igApiProvider = createIgApiProvider()
    fhirOperator = FhirOperator(fhirContext, fhirEngineApiProvider, igApiProvider)

    TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
  }

  private fun createIgApiProvider(): WorkflowApiProvider {
    val context: Context = ApplicationProvider.getApplicationContext()
    val igDb =
      Room.inMemoryDatabaseBuilder(context, ImplementationGuideDatabase::class.java).build()
    igManager = IgManager(igDb)
    igDependency = IgContext.ImplementationGuideDependency("sampledata/anc-cds", "0.3.0")

    val igContext = IgContext(listOf(igDependency))
    return object : WorkflowApiProvider {
      override fun fhirDal(): FhirDal = IgManagerFhirDal(igContext, igManager)

      override fun terminologyProvider(): TerminologyProvider =
        IgManagerTerminologyProvider(fhirContext, igContext, igManager)

      override fun libraryContentProvider(): LibrarySourceProvider =
        IgManagerLibraryContentProvider(igContext, igManager, AdapterFactory())

      override fun retrieveProvider(): RetrieveProvider =
        IgManagerRetrieveProvider(igContext, igManager)
    }
  }

  @Test
  fun evaluateIndividualSubjectMeasure() = runBlocking {
    igManager.import(
      igDependency,
      object : IgManager.FileRetriever {
        override fun listFiles(): List<Pair<InputStream, String>> {
          val fileList = this.javaClass.getResourceAsStream("/anc-cds/filelist")!!
          return InputStreamReader(fileList).readLines().map { fileName: String ->
            val stream = javaClass.getResourceAsStream("/anc-cds/$fileName")!!
            val fileUri = javaClass.getResource("/anc-cds/$fileName")!!.file
            stream to fileUri
          }
        }
      }
    )
    fhirEngine.run {
      loadFile("/first-contact/01-registration/patient-charity-otala-1.json")
      loadFile("/first-contact/02-enrollment/careplan-charity-otala-1-pregnancy-plan.xml")
      loadFile("/first-contact/02-enrollment/episodeofcare-charity-otala-1-pregnancy-episode.xml")
      loadFile("/first-contact/03-contact/encounter-anc-encounter-charity-otala-1.xml")
    }
    val measureReport =
      fhirOperator.evaluateMeasure(
        measureUrl = "http://fhir.org/guides/who/anc-cds/Measure/ANCIND01",
        start = "2020-01-01",
        end = "2020-01-31",
        reportType = MeasureEvalType.SUBJECT.toCode(),
        subject = "charity-otala-1",
        practitioner = "jane",
        lastReceivedOn = null
      )

    measureReport.date = null

    JSONAssert.assertEquals(
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
}
