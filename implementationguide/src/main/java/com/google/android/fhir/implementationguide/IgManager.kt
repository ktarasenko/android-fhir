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
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.implementationguide.db.impl.ImplementationGuideDatabase
import com.google.android.fhir.implementationguide.db.impl.entities.ImplementationGuideEntity
import com.google.android.fhir.implementationguide.db.impl.entities.ResourceEntity
import com.google.android.fhir.implementationguide.npm.NpmPackageManager
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.ImplementationGuide
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.MetadataResource
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.utilities.TextFile

class IgManager(private val igDatabase: ImplementationGuideDatabase) {

  private val fhirContext = FhirContext.forR4()
  private val jsonParser = fhirContext.newJsonParser()

  suspend fun loadLibrary(igContext: IgContext, name: String, version: String?): Library? {
    val resourceEntities =
      if (version != null) {
        igDatabase
          .implementationGuideDao()
          .getResourcesWithNameAndVersion(ResourceType.Library, name, version, getIgIds(igContext))
      } else {
        igDatabase
          .implementationGuideDao()
          .getResourcesWithName(ResourceType.Library, name, getIgIds(igContext))
      }

    return resourceEntities.firstOrNull()?.let { loadResource(it) } as Library?
  }

  suspend fun loadById(
    igContext: IgContext,
    resourceType: String,
    id: String,
  ): Iterable<IBaseResource> {
    val resourceEntities =
      igDatabase
        .implementationGuideDao()
        .getResourcesWithResourceId(ResourceType.fromCode(resourceType), id, getIgIds(igContext))

    return resourceEntities.map { loadResource(it) }
  }

  suspend fun loadAll(
    igContext: IgContext,
    resourceType: String,
    url: String? = null,
  ): Iterable<IBaseResource> {
    val resourceEntities =
      if (url != null) {
        igDatabase
          .implementationGuideDao()
          .getResourcesWithUrl(ResourceType.fromCode(resourceType), url, getIgIds(igContext))
      } else {
        igDatabase
          .implementationGuideDao()
          .getResources(ResourceType.fromCode(resourceType), getIgIds(igContext))
      }

    return resourceEntities.map { loadResource(it) }
  }

  private suspend fun getIgIds(igContext: IgContext): List<Long> {
    return igContext.dependencies
      .map { igDatabase.implementationGuideDao().getImplementationGuide(it.packageId, it.version) }
      .map { it.id }
  }

  private fun loadResource(resourceEntity: ResourceEntity): IBaseResource {
    val resource = jsonParser.parseResource(FileInputStream(resourceEntity.fileUri))
    // if (resource.fhirType() == ResourceType) TODO: handle bundles
    return resource
  }

  suspend fun import(
    igDependency: IgContext.ImplementationGuideDependency,
    fileRetriever: FileRetriever,
  ) {
    val igId =
      igDatabase
        .implementationGuideDao()
        .insert(
          ImplementationGuideEntity(0L, igDependency.packageId, igDependency.version, File(""))
        )
    fileRetriever.listFiles().forEach {
      val fileUri = it.second
      try {
        val resource = jsonParser.parseResource(it.first)
        when (resource) {
          is Bundle -> {
            // TODO: handle bundles
            // for (entry in resource.entry) {
            //   when (val bundleResource = entry.resource) {
            //     is MetadataResource -> importResource(igId, bundleResource, fileUri)
            //     else -> println("Can't import resource ${entry.resource.resourceType}")
            //   }
            // }
          }
          is Resource -> importResource(igId, resource, fileUri)
        }
      } catch (exception: Exception) {
        println(fileUri)
        println(exception)
      }
    }
  }

  private suspend fun importResource(igId: Long, resource: Resource, fileUri: String) {
    val metadataResource = resource as? MetadataResource
    val res =
      ResourceEntity(
        0L,
        resource.resourceType,
        resource.id,
        metadataResource?.url,
        metadataResource?.name,
        metadataResource?.version,
        File(fileUri),
        igId
      )
    igDatabase.implementationGuideDao().insert(res)
  }

  suspend fun loadIgs(
    androidContext: Context,
    dependencies: List<IgContext.ImplementationGuideDependency>,
  ) {
    val fakeIg =
      ImplementationGuide().apply {
        dependencies.forEach {
          addDependsOn(
            ImplementationGuide.ImplementationGuideDependsOnComponent().apply {
              packageId = it.packageId
              version = it.version
              uri = it.uri
            }
          )
        }
      }

    NpmPackageManager.fromResource(
        androidContext,
        fakeIg,
        /* version = */ "4.0.1",
        /* ...packageServers = */
        "https://packages.fhir.org",
        "https://packages.simplifier.net",
      )
      .npmList.forEach {
        val igEntity = ImplementationGuideEntity(0L, it.name(), it.version(), File(it.path))
        val igId = igDatabase.implementationGuideDao().insert(igEntity)
        it.loadAllFiles { file ->
          runBlocking { importFile(igId, file) }
          // TODO: why do we need it?
          TextFile.fileToBytes(file)
        }
      }
  }

  private suspend fun importFile(igId: Long, file: File) {
    FileInputStream(file).use { fileStream ->
      val resource = jsonParser.parseResource(fileStream)
      when (resource) {
        is Bundle -> {
          // TODO: handle bundles
          // for (entry in resource.entry) {
          //   when (val bundleResource = entry.resource) {
          //     is MetadataResource -> importResource(igId, bundleResource, fileUri)
          //     else -> println("Can't import resource ${entry.resource.resourceType}")
          //   }
          // }
        }
        is Resource -> importResource(igId, resource, file.absolutePath)
      }
    }
  }

  interface FileRetriever {
    fun listFiles(): List<Pair<InputStream, String>>
  }
}
