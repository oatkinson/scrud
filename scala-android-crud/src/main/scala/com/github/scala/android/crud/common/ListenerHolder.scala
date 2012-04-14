package com.github.scala.android.crud.common

import java.util.concurrent.ConcurrentHashMap
import collection.mutable.ConcurrentMap
import scala.collection.JavaConversions._
import scala.collection.Set

trait ListenerSet[L] {
  def listeners: Set[L]
}

/** A Listener holder
  * @author Eric Pabst (epabst@gmail.com)
  */

trait ListenerHolder[L] extends ListenerSet[L] {
  protected def listenerSet: ListenerSet[L]

  def listeners = listenerSet.listeners

  def addListener(listener: L)

  def removeListener(listener: L)
}

trait DelegatingListenerSet[L] extends ListenerSet[L] {
  protected def listenerSet: ListenerSet[L]

  def listeners: Set[L] = listenerSet.listeners
}

trait DelegatingListenerHolder[L] extends ListenerHolder[L] {
  protected def listenerHolder: ListenerHolder[L]

  protected def listenerSet = listenerHolder

  def addListener(listener: L) {
    listenerHolder.addListener(listener)
  }

  def removeListener(listener: L) {
    listenerHolder.removeListener(listener)
  }
}

trait EmptyListenerSet[L] extends ListenerSet[L] {
  override def listeners = Set.empty
}

trait NoopListenerHolder[L] extends ListenerHolder[L] with EmptyListenerSet[L] {
  protected def listenerSet = this

  def addListener(listener: L) {
    // do nothing since no events will ever notify a listener
  }

  def removeListener(listener: L) {
    // do nothing since no events will ever notify a listener
  }
}

class MutableListenerSet[L] extends ListenerSet[L] with ListenerHolder[L] {
  private val theListeners: ConcurrentMap[L,L] = new ConcurrentHashMap[L,L]()

  protected val listenerSet: ListenerSet[L] = new ListenerSet[L] {
    def listeners = theListeners.keySet
  }

  def addListener(listener: L) {
    theListeners += listener -> listener
  }

  def removeListener(listener: L) {
    theListeners -= listener
  }
}

trait SimpleListenerHolder[L] extends DelegatingListenerHolder[L] {
  protected lazy val listenerHolder: MutableListenerSet[L] = new MutableListenerSet[L]
}
