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
import org.opencds.cqf.cql.engine.retrieve.TerminologyAwareRetrieveProvider
import org.opencds.cqf.cql.engine.runtime.Code
import org.opencds.cqf.cql.engine.runtime.Interval

class IgManagerRetrieveProvider(
  private val igContext: IgContext,
  private val igManager: IgManager
) : TerminologyAwareRetrieveProvider() {
  override fun retrieve(
    context: String?,
    contextPath: String?,
    contextValue: Any?,
    dataType: String?,
    templateId: String?,
    codePath: String?,
    codes: Iterable<Code>?,
    valueSet: String?,
    datePath: String?,
    dateLowPath: String?,
    dateHighPath: String?,
    dateRange: Interval?,
  ): Iterable<Any> = runBlocking {
    if (dataType == null) {
      emptyList()
    } else if (contextPath == "id" && contextValue == null) {
      emptyList()
    } else if (contextPath == "id" && contextValue != null) {
      igManager.loadById(igContext, dataType, contextValue as String)
    } else {
      igManager.loadAll(igContext, dataType).take(1) // That's wrong
    }
  }
}
