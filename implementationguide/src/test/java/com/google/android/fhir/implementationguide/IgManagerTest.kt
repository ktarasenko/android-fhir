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

package com.google.android.fhir.implementationguide

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.implementationguide.data.IgManagerFhirDal
import com.google.android.fhir.implementationguide.data.IgManagerLibraryContentProvider
import com.google.android.fhir.implementationguide.data.IgManagerRetrieveProvider
import com.google.android.fhir.implementationguide.data.IgManagerTerminologyProvider
import com.google.android.fhir.implementationguide.db.impl.ImplementationGuideDatabase
import com.google.common.truth.Truth.assertThat
import java.io.InputStream
import java.io.InputStreamReader
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.opencds.cqf.cql.engine.data.CompositeDataProvider
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver
import org.opencds.cqf.cql.evaluator.engine.model.CachingModelResolverDecorator
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory
import org.opencds.cqf.cql.evaluator.measure.common.MeasureEvalType
import org.opencds.cqf.cql.evaluator.measure.r4.R4MeasureProcessor
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IgManagerTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val igDb =
    Room.inMemoryDatabaseBuilder(context, ImplementationGuideDatabase::class.java).build()
  private val igManager = IgManager(igDb)

  @Test
  fun importFullIg() {
    val igDependency = IgContext.ImplementationGuideDependency("anc-cds", "0.3.0")
    runBlocking {
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

      assertThat(
          igDb
            .implementationGuideDao()
            .getResources(
              listOf(igDb.implementationGuideDao().getImplementationGuide("anc-cds", "0.3.0").id)
            )
            .size
        )
        .isGreaterThan(100)

      val fhirContext = FhirContext.forR4()
      val igContext = IgContext(listOf(igDependency))
      val measureProcessor =
        R4MeasureProcessor(
          IgManagerTerminologyProvider(fhirContext, igContext, igManager),
          IgManagerLibraryContentProvider(igContext, igManager, AdapterFactory()),
          CompositeDataProvider(
            CachingModelResolverDecorator(R4FhirModelResolver()),
            IgManagerRetrieveProvider(igContext, igManager)
          ),
          IgManagerFhirDal(igContext, igManager)
        )

      val measureReport =
        measureProcessor.evaluateMeasure(
          "http://fhir.org/guides/who/anc-cds/Measure/ANCIND01",
          "2019-01-01",
          "2021-12-31",
          MeasureEvalType.POPULATION.toCode(),
          null,
          null,
          null,
          null,
          null,
          null,
          null
        )

      assertThat(measureReport).isNotNull()
    }
  }
}
