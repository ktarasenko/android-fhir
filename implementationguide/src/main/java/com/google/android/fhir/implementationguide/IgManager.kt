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
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.implementationguide.db.impl.ImplementationGuideDatabase
import com.google.android.fhir.implementationguide.db.impl.entities.ResourceMetadataEntity
import com.google.android.fhir.implementationguide.db.impl.entities.toEntity
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.MetadataResource
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import timber.log.Timber

/** Responsible for downloading, accessing and deleting Implementation Guides. */
class IgManager internal constructor(private val igDatabase: ImplementationGuideDatabase) {

  private val igDao = igDatabase.implementationGuideDao()
  private val jsonParser = FhirContext.forR4().newJsonParser()
  private var defaultIgId: Long = -1L

  /**
   * * Checks if the [igDependencies] are present in DB. If necessary, downloads the dependencies
   * from NPM and imports data from the package manager.
   */
  suspend fun install(vararg igDependencies: IgDependency) {
    TODO("not implemented yet")
  }

  /**
   * Checks if the [igDependency] is present in DB. If necessary, imports the IG from the provided
   * [rootDirectory]
   */
  suspend fun install(igDependency: IgDependency, rootDirectory: File) {
    val igId = igDao.insert(igDependency.toEntity(rootDirectory))
    rootDirectory.listFiles()?.forEach { file -> importFile(igId, file) }
  }

  /** Imports the IG from the provided [file] to the default dependency. */
  suspend fun install(file: File) {
    // lazy suspend initialization
    defaultIgId = igDao.getImplementationGuide(DEFAULT_DEPENDENCY.packageId,
                                               DEFAULT_DEPENDENCY.version)?.implementationGuideId
      ?: -1
    if (defaultIgId == -1L) {
      defaultIgId = igDao.insert(DEFAULT_DEPENDENCY.toEntity(File("NotARealDirectory")))
    }
    importFile(defaultIgId, file)
  }

  /** Loads resources from IGs listed in dependencies. */
  suspend fun loadResources(
    resourceType: String,
    url: String? = null,
    name: String? = null,
    version: String? = null,
  ): Iterable<IBaseResource> {
    val resType = ResourceType.fromCode(resourceType)
    val resourceEntities =
      when {
        url != null -> listOfNotNull(igDao.getResourceWithUrl(url))
        name != null && version != null ->
          listOfNotNull(igDao.getResourcesWithNameAndVersion(resType, name, version))
        name != null -> igDao.getResourcesWithName(resType, name)
        else -> igDao.getResources(resType)
      }
    return resourceEntities.map { loadResource(it) }
  }

  /** Deletes Implementation Guide, cleans up files. */
  suspend fun delete(vararg igDependencies: IgDependency) {
    igDependencies.forEach { igDependency ->
      val igEntity = igDao.getImplementationGuide(igDependency.packageId, igDependency.version)
      if (igEntity != null) {
        igDao.deleteImplementationGuide(igEntity)
        igEntity.rootDirectory.deleteRecursively()
      }
    }
  }

  private suspend fun importFile(igId: Long, file: File) {
    val resource = withContext(Dispatchers.IO) {
      try {
        FileInputStream(file).use(jsonParser::parseResource)
      } catch (exception: Exception) {
        Timber.d(exception, "Unable to import file: %file")
      }
    }
    when (resource) {
      is Bundle -> importBundle(igId, resource, file)
      is Resource -> importResource(igId, resource, file)
    }
  }


  private suspend fun importBundle(igId: Long, resource: Bundle, file: File) {
    // TODO: Multiple resources will point to the same file, and it needs to be accounted for while
    // reading it.
    // for (entry in resource.entry) {
    //   when (val bundleResource = entry.resource) {
    //     is Resource -> importResource(igId, bundleResource, fileUri)
    //     else -> println("Can't import resource ${entry.resource.resourceType}")
    //   }
    // }
  }

  private suspend fun importResource(igId: Long, resource: Resource, file: File) {
    val metadataResource = resource as? MetadataResource
    val res =
      ResourceMetadataEntity(
        0L,
        resource.resourceType,
        metadataResource?.url,
        metadataResource?.name,
        metadataResource?.version,
        file
      )
    igDao.insertResource(igId, res)
  }

  private fun loadResource(resourceEntity: ResourceMetadataEntity): IBaseResource {
    return jsonParser.parseResource(FileInputStream(resourceEntity.resourceFile))
  }

  fun close() {
    igDatabase.close()
  }

  companion object {
    val DEFAULT_DEPENDENCY =
      IgDependency("com.google.android.fhir", "1.0.0", "http://github.com/google/android-fhir")

    fun create(context: Context) = IgManager(Room.databaseBuilder(context,
                                                                  ImplementationGuideDatabase::class.java,
                                                                  "implementationguide.db")
                                               .build())

    fun createInMemory(context: Context) =
      IgManager(Room.inMemoryDatabaseBuilder(context, ImplementationGuideDatabase::class.java)
                  .build())

  }
}
