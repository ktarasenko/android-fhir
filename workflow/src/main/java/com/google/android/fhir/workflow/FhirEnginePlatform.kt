/*
 * Copyright 2021 Google LLC
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

import org.cqframework.fhir.api.FhirCapabilities
import org.cqframework.fhir.api.FhirDal
import org.cqframework.fhir.api.FhirPlatform
import org.cqframework.fhir.api.FhirService
import org.cqframework.fhir.api.FhirTransactions

class FhirEnginePlatform : FhirPlatform {
  override fun dal(): FhirDal {
    TODO("Not yet implemented")
  }

  override fun capabilities(): FhirCapabilities {
    TODO("Not yet implemented")
  }

  override fun transactions(): FhirTransactions {
    TODO("Not yet implemented")
  }

  override fun <T : FhirService?> getService(): T {
    TODO("Not yet implemented")
  }
}
