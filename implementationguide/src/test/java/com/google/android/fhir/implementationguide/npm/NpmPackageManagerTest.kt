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

package com.google.android.fhir.implementationguide.npm

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.implementationguide.IgContext
import com.google.android.fhir.implementationguide.IgManager
import com.google.android.fhir.implementationguide.db.impl.ImplementationGuideDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NpmPackageManagerTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val igDb =
    Room.inMemoryDatabaseBuilder(context, ImplementationGuideDatabase::class.java).build()
  private val igManager = IgManager(igDb)
  private val dependencies =
    listOf(
      IgContext.ImplementationGuideDependency("hl7.fhir.us.cqfmeasures", "3.0.0", null),
      IgContext.ImplementationGuideDependency(
        "fhir.cdc.opioid-mme-r4",
        "3.0.0",
        "http://fhir.org/guides/cdc/opioid-mme-r4"
      )
    )

  @Test fun downloadIG() = runBlocking { igManager.loadIgs(context, dependencies) }
}
