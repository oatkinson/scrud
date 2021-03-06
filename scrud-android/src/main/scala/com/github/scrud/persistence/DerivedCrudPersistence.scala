package com.github.scrud.persistence

import com.github.scrud.state.DestroyStateListener
import com.github.scrud.util.{DelegatingListenerSet, ListenerSet}
import com.github.scrud.{EntityName, CrudContext}

/** A CrudPersistence that is derived from related CrudType persistence(s).
  * @author Eric Pabst (epabst@gmail.com)
  * @see DerivedPersistenceFactory
  */
abstract class DerivedCrudPersistence[T <: AnyRef](val crudContext: CrudContext,
                                                   protected val listenerSet: ListenerSet[DataListener],
                                                   delegates: EntityName*)
        extends SeqCrudPersistence[T] with ReadOnlyPersistence with DelegatingListenerSet[DataListener] {
  {
    val listenerForDelegateChanges = NotifyDataListenerSetListener(listenerSet)
    delegates.foreach { delegate =>
      crudContext.dataListenerHolder(delegate).addListenerIfNotPresent(listenerForDelegateChanges)
    }
    crudContext.applicationState.addListener(new DestroyStateListener {
      def onDestroyState() {
        delegates.foreach { delegate =>
          crudContext.dataListenerHolder(delegate).removeListener(listenerForDelegateChanges)
        }
      }
    })
  }

  val delegatePersistenceMap: Map[EntityName,CrudPersistence] =
    delegates.map(delegate => delegate -> crudContext.openEntityPersistence(delegate)).toMap

  override def close() {
    delegatePersistenceMap.values.foreach(_.close())
    super.close()
  }
}
