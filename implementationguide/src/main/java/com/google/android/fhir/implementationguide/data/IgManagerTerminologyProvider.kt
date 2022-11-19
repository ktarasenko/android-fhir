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

import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.implementationguide.IgContext
import com.google.android.fhir.implementationguide.IgManager
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.ValueSet
import org.opencds.cqf.cql.engine.exception.TerminologyProviderException
import org.opencds.cqf.cql.engine.runtime.Code
import org.opencds.cqf.cql.engine.terminology.CodeSystemInfo
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo
import org.opencds.cqf.cql.evaluator.engine.util.ValueSetUtil

class IgManagerTerminologyProvider(
  private val fhirContext: FhirContext,
  private val igContext: IgContext,
  private val igManager: IgManager,
) : TerminologyProvider {

  override fun `in`(code: Code, valueSet: ValueSetInfo): Boolean {
    try {
      return expand(valueSet).any { it.code == code.code && it.system == code.system }
    } catch (e: Exception) {
      throw TerminologyProviderException(
        "Error performing membership check of Code: $code in ValueSet: ${valueSet.id}",
        e
      )
    }
  }

  override fun expand(valueSetInfo: ValueSetInfo): MutableIterable<Code> {
    try {
      val valueSet = resolveValueSet(valueSetInfo)
      return ValueSetUtil.getCodesInExpansion(fhirContext, valueSet)
        ?: ValueSetUtil.getCodesInCompose(fhirContext, valueSet)
    } catch (e: Exception) {
      throw TerminologyProviderException(
        "Error performing expansion of ValueSet: ${valueSetInfo.id}",
        e
      )
    }
  }

  override fun lookup(code: Code, codeSystem: CodeSystemInfo): Code {
    TODO("not implemented yet")
  }

  private fun searchByUrl(url: String?): List<ValueSet> {
    if (url == null) return emptyList()
    return runBlocking {
      igManager.loadAll(igContext, ResourceType.ValueSet.name, url).map { it as ValueSet }
    }
  }

  private fun searchByIdentifier(identifier: String?): List<ValueSet> = runBlocking {
    if (identifier == null) {
      emptyList()
    } else {
      igManager.loadById(igContext, ResourceType.ValueSet.name, identifier).map { it as ValueSet }
    }
  }

  private fun resolveValueSet(valueSet: ValueSetInfo): ValueSet {
    if (valueSet.version != null ||
        (valueSet.codeSystems != null && valueSet.codeSystems.isNotEmpty())
    ) {
      // Cannot do both at the same time yet.
      throw UnsupportedOperationException(
        "Could not expand value set ${valueSet.id}; version and code system bindings are not supported at this time."
      )
    }

    var searchResults = searchByUrl(valueSet.id)
    if (searchResults.isEmpty()) {
      searchResults = searchByIdentifier(valueSet.id)
    }

    return if (searchResults.isEmpty()) {
      throw IllegalArgumentException("Could not resolve value set ${valueSet.id}.")
    } else if (searchResults.size > 1) {
      throw IllegalArgumentException("Found more than 1 ValueSet with url: ${valueSet.id}")
    } else {
      searchResults.first()
    }
  }

  fun resolveValueSetId(valueSet: ValueSetInfo): String {
    return resolveValueSet(valueSet).idElement.idPart
  }
}
