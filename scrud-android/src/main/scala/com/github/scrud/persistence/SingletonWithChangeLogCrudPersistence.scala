package com.github.scrud.persistence

import com.github.scrud.util.{CachedFunction, ListenerSet, ListenerHolder, DelegatingListenerSet}
import com.github.scrud.UriPath
import com.github.scrud.platform.PlatformTypes

/**
 * A CrudPersistence that wraps another one, but only returning the first one,
 * and creating a new instance instead of updating an existing instance.
 */
case class SingletonWithChangeLogCrudPersistence(manyPersistence: CrudPersistence,
                                                 listenerHolder: ListenerHolder[DataListener]) extends CrudPersistence
    with DelegatingListenerSet[DataListener] {

  protected def listenerSet: ListenerSet[DataListener] = manyPersistence

  val entityType = manyPersistence.entityType

  private lazy val cacheClearingListener = new DataListener {
    def onChanged(uri: UriPath) { cachedFindAll.clear() }
  }

  private lazy val cachedFindAll: CachedFunction[UriPath,Seq[AnyRef]] = {
    val function = CachedFunction((uri: UriPath) => manyPersistence.findAll(uri).take(1))
    listenerHolder.addListener(cacheClearingListener)
    function
  }

  def findAll(uri: UriPath) = cachedFindAll(uri)

  def newWritable() = manyPersistence.newWritable()

  // Intentionally save without reusing the ID so that existing instances are never modified since a change-log
  def doSave(id: Option[PlatformTypes.ID], writable: AnyRef) = {
    val writableWithoutId = entityType.idPkField.updateWithValue(writable, None)
    manyPersistence.doSave(None, writableWithoutId)
  }

  def doDelete(uri: UriPath) {
    manyPersistence.doDelete(uri)
  }

  def close() {
    manyPersistence.close()
    listenerHolder.removeListener(cacheClearingListener)
  }
}
