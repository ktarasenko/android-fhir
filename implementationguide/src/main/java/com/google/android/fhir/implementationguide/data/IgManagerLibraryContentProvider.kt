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

package com.google.android.fhir.implementationguide.data

import com.google.android.fhir.implementationguide.IgContext
import com.google.android.fhir.implementationguide.IgManager
import kotlinx.coroutines.runBlocking
import org.hl7.elm.r1.VersionedIdentifier
import org.hl7.fhir.instance.model.api.IBaseResource
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BaseFhirLibrarySourceProvider
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory

class IgManagerLibraryContentProvider(
  private val igContext: IgContext,
  private val igManager: IgManager,
  adapterFactory: AdapterFactory,
) : BaseFhirLibrarySourceProvider(adapterFactory) {
  override fun getLibrary(libraryIdentifier: VersionedIdentifier): IBaseResource? = runBlocking {
    igManager.loadLibrary(igContext, libraryIdentifier.id, libraryIdentifier.version)
  }
}
