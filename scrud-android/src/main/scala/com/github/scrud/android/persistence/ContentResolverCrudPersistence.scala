package com.github.scrud.android.persistence

import com.github.scrud.persistence._
import com.github.scrud.{UriPath, EntityType}
import android.content.{ContentResolver, ContentValues}
import com.github.scrud.platform.PlatformTypes._
import com.github.scrud.android.view.AndroidConversions._
import com.github.scrud.util.{DelegatingListenerSet, MutableListenerSet}
import scala.Some
import com.github.scrud.android.view.AndroidConversions
import android.net.Uri

/**
 * A [[com.github.scrud.persistence.CrudPersistence]] that delegates to ContentResolver.
 * ic Pabst (epabst@gmail.com)
 * Date: 4/9/13
 * Time: 3:57 PM
 */
class ContentResolverCrudPersistence(val entityType: EntityType, contentResolver: ContentResolver,
                                     persistenceFactoryMapping: PersistenceFactoryMapping,
                                     protected val listenerSet: MutableListenerSet[DataListener])
    extends CrudPersistence with DelegatingListenerSet[DataListener] {
  private lazy val entityTypePersistedInfo = EntityTypePersistedInfo(entityType)
  private lazy val queryFieldNames = entityTypePersistedInfo.queryFieldNames.toArray
  private lazy val uriPathWithEntityName = UriPath(entityType.entityName)
  private lazy val applicationUri = AndroidConversions.baseUriFor(persistenceFactoryMapping)

  private def toUri(uriPath: UriPath): Uri = {
    AndroidConversions.withAppendedPath(applicationUri, uriPath)
  }

  def findAll(uriPath: UriPath) = {
    val uri = toUri(uriPath)
    val cursor = Option(contentResolver.query(uri, queryFieldNames, null, Array.empty, null)).getOrElse {
      sys.error("Error resolving content: " + uri)
    }
    CursorStream(cursor, entityTypePersistedInfo)
  }

  def newWritable() = ContentResolverPersistenceFactory.newWritable()

  def doSave(idOpt: Option[ID], writable: AnyRef) = idOpt match {
    case Some(id) =>
      contentResolver.update(toUri(uriPathWithEntityName / id), writable.asInstanceOf[ContentValues], null, Array.empty)
      id
    case None =>
      val newUri: UriPath = contentResolver.insert(toUri(uriPathWithEntityName), writable.asInstanceOf[ContentValues])
      newUri.findId(entityType.entityName).get
  }

  /** @return how many were deleted */
  def doDelete(uri: UriPath) = {
    contentResolver.delete(toUri(uri), null, Array.empty)
  }

  def close() {}
}