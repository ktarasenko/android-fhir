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

package com.google.android.fhir.datacapture.common.datatype

import org.hl7.fhir.r4.model.PrimitiveType
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Type

/**
 * Returns the string representation of a [PrimitiveType].
 *
 * <p>If the type isn't a [PrimitiveType], an empty string is returned.
 */
fun Type.asStringValue(): String {
  if (!isPrimitive) return ""
  return (this as PrimitiveType<*>).asStringValue()
}

fun QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent.isNotValid(): Boolean =
  when {
    hasValueStringType() && valueStringType.value.isNullOrEmpty() -> true
    hasValueBooleanType() && valueBooleanType.isEmpty -> true
    hasValueDecimalType() && valueDecimalType.isEmpty -> true
    hasValueDateType() && valueDateType.isEmpty -> true
    hasValueDateTimeType() && valueDateTimeType.isEmpty -> true
    hasValueQuantity() && valueQuantity.isEmpty -> true
    hasValueCoding() && valueCoding.isEmpty -> true
    hasValue() && value.isEmpty -> true
    hasValueUriType() && valueUriType.isEmpty -> true
    hasValueAttachment() && valueAttachment.isEmpty -> true
    else -> false
  }
