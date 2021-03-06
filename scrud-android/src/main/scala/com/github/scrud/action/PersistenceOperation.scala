package com.github.scrud.action

import com.github.scrud.{CrudContext, UriPath, EntityType}
import com.github.scrud.persistence.CrudPersistence

/** An operation that interacts with an entity's persistence.
  * The CrudContext is available as persistence.crudContext to implementing classes.
  * @author Eric Pabst (epabst@gmail.com)
  */
abstract class PersistenceOperation(entityType: EntityType) extends Operation {
  def invoke(uri: UriPath, persistence: CrudPersistence, crudContext: CrudContext)

  /** Runs the operation, given the uri and the current CrudContext. */
  def invoke(uri: UriPath, crudContext: CrudContext) {
    crudContext.withEntityPersistence(entityType) { persistence =>
      invoke(uri, persistence, crudContext)
    }
  }
}

