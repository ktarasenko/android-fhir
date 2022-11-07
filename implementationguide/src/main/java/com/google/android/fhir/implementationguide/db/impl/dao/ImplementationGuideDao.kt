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

package com.google.android.fhir.implementationguide.db.impl.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.google.android.fhir.implementationguide.db.impl.entities.ImplementationGuideEntity
import com.google.android.fhir.implementationguide.db.impl.entities.ResourceEntity

@Dao
internal abstract class ImplementationGuideDao {

  @Query("SELECT * from ImplementationGuideEntity")
  internal abstract suspend fun getImplementationGuides(): List<ImplementationGuideEntity>

  @Query("SELECT * from ImplementationGuideEntity WHERE name = :name AND version = :version")
  internal abstract suspend fun getImplementationGuide(
    name: String,
    version: String
  ): ImplementationGuideEntity

  @Query("SELECT * from ResourceEntity WHERE implementationGuideId IN (:igId)")
  internal abstract suspend fun getResourcesByImplementationGuide(
    vararg igId: Long
  ): List<ResourceEntity>

  @Delete
  internal abstract suspend fun deleteImplementationGuide(igEntity: ImplementationGuideEntity)

  @Query("DELETE FROM ResourceEntity WHERE  implementationGuideId = :igId")
  internal abstract suspend fun deleteResources(igId: Long)

  @Transaction
  open suspend fun deleteImplementationGuide(name: String, version: String) {
    val igEntity = getImplementationGuide(name, version)
    deleteResources(igEntity.id)
    deleteImplementationGuide(igEntity)
  }

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  abstract suspend fun insert(resourceEntity: ResourceEntity): Long

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  abstract suspend fun insert(ig: ImplementationGuideEntity): Long
}
