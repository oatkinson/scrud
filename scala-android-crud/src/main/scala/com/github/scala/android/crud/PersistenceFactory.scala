package com.github.scala.android.crud

import common.{ListenerHolder, UriPath}
import persistence.{PersistenceListener, EntityType}

/** A factory for EntityPersistence specific to a storage type such as SQLite.
  * @author Eric Pabst (epabst@gmail.com)
  */

trait PersistenceFactory {
  /** Indicates if an entity can be saved. */
  def canSave: Boolean

  /** Indicates if an entity can be deleted. */
  def canDelete: Boolean = canSave

  /** Indicates if an entity can be listed. */
  def canList: Boolean = true

  /** Instantiates a data buffer which can be saved by EntityPersistence.
    * The EntityType must support copying into this object.
    */
  def newWritable: AnyRef

  def createEntityPersistence(entityType: EntityType, crudContext: CrudContext): CrudPersistence

  /** Returns true if the URI is worth calling EntityPersistence.find to try to get an entity instance.
    * It may be overridden in cases where an entity instance can be found even if no ID is present in the URI.
    */
  def maySpecifyEntityInstance(entityType: EntityType, uri: UriPath): Boolean =
    entityType.IdField.getter(uri).isDefined

  final def addListener(listener: PersistenceListener, entityType: EntityType, crudContext: CrudContext) {
    listenerHolder(entityType, crudContext).addListener(listener)
  }

  def listenerHolder(entityType: EntityType, crudContext: CrudContext): ListenerHolder[PersistenceListener]
}
