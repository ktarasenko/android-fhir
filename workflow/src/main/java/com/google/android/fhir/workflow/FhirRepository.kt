package com.google.android.fhir.workflow

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.IQueryParameterType
import ca.uhn.fhir.rest.api.MethodOutcome
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseConformance
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IIdType
import org.opencds.cqf.fhir.api.Repository

class FhirRepository: Repository {
  override fun <T : IBaseResource, I : IIdType> read(
    resourceType: Class<T>,
    id: I,
    headers: Map<String, String>,
  ): T {
    TODO("Not yet implemented")
  }

  override fun <T : IBaseResource?> create(
    resource: T,
    headers: Map<String, String>,
  ): MethodOutcome {
    TODO("Not yet implemented")
  }

  override fun <I : IIdType, P : IBaseParameters> patch(
    id: I,
    patchParameters: P,
    headers: Map<String, String>,
  ): MethodOutcome {
    TODO("Not yet implemented")
  }

  override fun <T : IBaseResource?> update(
    resource: T,
    headers: Map<String, String>,
  ): MethodOutcome {
    TODO("Not yet implemented")
  }

  override fun <T : IBaseResource?, I : IIdType> delete(
    resourceType: Class<T>,
    id: I,
    headers: Map<String, String>,
  ): MethodOutcome {
    TODO("Not yet implemented")
  }

  override fun <B : IBaseBundle, T : IBaseResource> search(
    bundleType: Class<B>,
    resourceType: Class<T>,
    searchParameters: Map<String, List<IQueryParameterType>>,
    headers: Map<String, String>,
  ): B {
    TODO("Not yet implemented")
  }

  override fun <B : IBaseBundle> link(
    bundleType: Class<B>,
    url: String,
    headers: Map<String, String>,
  ): B {
    TODO("Not yet implemented")
  }

  override fun <C : IBaseConformance?> capabilities(
    resourceType: Class<C>,
    headers: Map<String, String>,
  ): C {
    TODO("Not yet implemented")
  }

  override fun <B : IBaseBundle?> transaction(
    transaction: B,
    headers: Map<String, String>,
  ): B {
    TODO("Not yet implemented")
  }

  override fun <R : IBaseResource, P : IBaseParameters?> invoke(
    name: String,
    parameters: P,
    returnType: Class<R>,
    headers: Map<String, String>?,
  ): R {
    TODO("Not yet implemented")
  }

  override fun <P : IBaseParameters> invoke(
    name: String,
    parameters: P,
    headers: Map<String, String>,
  ): MethodOutcome {
    TODO("Not yet implemented")
  }

  override fun <R : IBaseResource?, P : IBaseParameters, T : IBaseResource> invoke(
    resourceType: Class<T>,
    name: String,
    parameters: P,
    returnType: Class<R>,
    headers: Map<String, String>?,
  ): R {
    TODO("Not yet implemented")
  }

  override fun <P : IBaseParameters, T : IBaseResource> invoke(
    resourceType: Class<T>,
    name: String,
    parameters: P,
    headers: Map<String, String>,
  ): MethodOutcome {
    TODO("Not yet implemented")
  }

  override fun <R : IBaseResource, P : IBaseParameters, I : IIdType> invoke(
    id: I,
    name: String,
    parameters: P,
    returnType: Class<R>,
    headers: Map<String, String>,
  ): R {
    TODO("Not yet implemented")
  }

  override fun <P : IBaseParameters, I : IIdType> invoke(
    id: I,
    name: String,
    parameters: P,
    headers: Map<String, String>,
  ): MethodOutcome {
    TODO("Not yet implemented")
  }

  override fun <B : IBaseBundle, P : IBaseParameters> history(
    parameters: P,
    returnType: Class<B>,
    headers: Map<String, String>,
  ): B {
    TODO("Not yet implemented")
  }

  override fun <B : IBaseBundle, P : IBaseParameters, T : IBaseResource> history(
    resourceType: Class<T>,
    parameters: P,
    returnType: Class<B>,
    headers: Map<String, String>,
  ): B {
    TODO("Not yet implemented")
  }

  override fun <B : IBaseBundle, P : IBaseParameters, I : IIdType> history(
    id: I,
    parameters: P,
    returnType: Class<B>,
    headers: Map<String, String>,
  ): B {
    TODO("Not yet implemented")
  }

  override fun fhirContext(): FhirContext {
    TODO("Not yet implemented")
  }
}