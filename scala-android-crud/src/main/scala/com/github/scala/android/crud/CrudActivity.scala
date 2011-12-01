package com.github.scala.android.crud

import action.{Operation, EntityOperation}
import android.os.Bundle
import com.github.triangle.JavaUtil.toRunnable

/**
 * A generic Activity for CRUD operations
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/3/11
 * Time: 7:06 AM
 */
class CrudActivity(val entityType: CrudType, val application: CrudApplication) extends BaseCrudActivity {

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(entityType.entryLayout)
    val currentPath = currentUriPath
    val contextItems = List(currentPath, crudContext, Unit)
    if (entityType.IdField.getter(currentPath).isDefined) {
      future {
        withPersistence { persistence =>
          val readableOrUnit: AnyRef = persistence.find(currentPath).getOrElse(Unit)
          val portableValue = entityType.copyFromItem(readableOrUnit :: contextItems)
          runOnUiThread { portableValue.copyTo(this) }
        }
      }
    } else {
      entityType.copyFromItem(Unit :: contextItems, this)
    }
  }

  override def onPause() {
    //intentionally don't include CrudContext presumably those are only used for calculated fields, which shouldn't be persisted.
    val contextItems = List(currentUriPath, Unit)
    val writable = entityType.newWritable
    withPersistence { persistence =>
      val transformedWritable = entityType.transformWithItem(writable, this :: contextItems)
      saveForOnPause(persistence, transformedWritable)
    }
    super.onPause()
  }

  private[crud] def saveForOnPause(persistence: CrudPersistence, writable: AnyRef) {
    try {
      val id = entityType.IdField.getter(currentUriPath)
      val newId = persistence.save(id, writable)
      if (id.isEmpty) setIntent(getIntent.setData(Operation.toUri(uriWithId(newId))))
    } catch { case e => error("onPause: Unable to store " + writable, e) }
  }

  protected def normalActions = entityType.getEntityActions(application).filter {
    case action: EntityOperation => action.entityName != entityType.entityName || action.action != currentAction
    case _ => true
  }
}
