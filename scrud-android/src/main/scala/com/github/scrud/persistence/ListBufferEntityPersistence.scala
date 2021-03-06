package com.github.scrud.persistence

import com.github.scrud.util.ListenerSet
import com.github.triangle.{Setter, Getter, Field}
import com.github.scrud.platform.PlatformTypes._
import com.github.scrud.{EntityName, UriPath, MutableIdPk, IdPk}
import com.github.scrud.android.persistence.CursorField
import collection.mutable
import java.util.concurrent.atomic.AtomicLong

/**
 * An EntityPersistence stored in-memory.
 * @author Eric Pabst (epabst@gmail.com)
 *         Date: 10/20/12
 *         Time: 4:57 PM
 */
class ListBufferEntityPersistence[T <: AnyRef](entityName: EntityName, newWritableFunction: => T,
                                               listenerSet: ListenerSet[DataListener]) extends SeqEntityPersistence[T] {
  private object IdField extends Field[ID](Getter[IdPk,ID](_.id).withUpdater(e => e.withId(_)) +
      Setter((e: MutableIdPk) => e.id = _) + CursorField.PersistedId)
  val buffer = mutable.ListBuffer[T]()

  // not a val since dynamic
  def listeners = listenerSet.listeners

  private val nextId = new AtomicLong(10000L)

  def toUri(id: ID) = entityName.toUri(id)

  def findAll(uri: UriPath) =
    uri.findId(entityName).map(id => buffer.toList.filter(item => IdField(item) == Some(id))).getOrElse(buffer.toList)

  def newWritable() = newWritableFunction

  def doSave(id: Option[ID], item: AnyRef) = {
    val newId = id.getOrElse {
      nextId.incrementAndGet()
    }
    // Prepend so that the newest ones come out first in results
    buffer.prepend(IdField.updateWithValue(item.asInstanceOf[T], Some(newId)))
    newId
  }

  def doDelete(uri: UriPath) {
    findAll(uri).foreach(entity => buffer -= entity)
  }

  def close() {}

  override def toString = super.toString + "[" + buffer.mkString(",") + "]"
}
