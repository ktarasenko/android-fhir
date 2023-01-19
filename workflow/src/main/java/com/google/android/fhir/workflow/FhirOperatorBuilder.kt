package com.google.android.fhir.workflow

import android.content.Context
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.implementationguide.IgDependency
import com.google.android.fhir.implementationguide.IgManager

class FhirOperatorBuilder(private val applicationContext: Context) {
  private var fhirContext: FhirContext? = null
  private var fhirEngine: FhirEngine? = null
  private var igDependencies: Array<out IgDependency> = emptyArray()
  private val igManager: IgManager? = null

  fun withFhirEngine(fhirEngine: FhirEngine): FhirOperatorBuilder {
    this.fhirEngine = fhirEngine
    return this
  }

  fun withFhirContext(fhirContext: FhirContext): FhirOperatorBuilder {
    this.fhirContext = fhirContext
    return this
  }

  fun withDependencies(vararg igDependencies: IgDependency): FhirOperatorBuilder {
    this.igDependencies = igDependencies
    return this
  }

  fun build(): FhirOperator {
    return FhirOperator(
      fhirContext ?: FhirContext(FhirVersionEnum.R4),
      fhirEngine ?: FhirEngineProvider.getInstance(applicationContext),
      igManager ?:  IgManager.create(applicationContext)
    )
  }

}