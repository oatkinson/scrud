package com.github.scrud.android.view

import com.github.triangle.{UpdaterInput, GetterInput, Logging}
import com.github.scrud.android.persistence.EntityType
import android.view.{ViewGroup, View}
import com.github.scrud.android.{CrudContext, CrudContextField, AndroidPlatformDriver, CachedStateListener}
import android.os.Bundle
import android.widget.{Adapter, AdapterView, BaseAdapter}
import com.github.scrud.android.common._
import com.github.scrud.android.common.PlatformTypes._
import scala.Some

trait AdapterCaching extends Logging with Timing { self: BaseAdapter =>
  def platformDriver: AndroidPlatformDriver

  def entityType: EntityType

  protected def logTag = entityType.logTag

  /** The UriPath that does not contain the entities. */
  protected def uriPathWithoutEntityId: UriPath

  protected lazy val baseUriPath: UriPath = uriPathWithoutEntityId.specify(entityType.entityName)

  lazy val IdField = entityType.IdField

  def getItemId(item: AnyRef, position: Int): ID = item match {
    case IdField(Some(id)) => id
    case _ => position
  }

  protected[scrud] def bindViewFromCacheOrItems(view: View, position: Int, parent: ViewGroup, crudContext: CrudContext, contextItems: GetterInput) {
    bindViewFromCacheOrItems(view, position, getItem(position), parent, crudContext, contextItems)
  }

  protected[scrud] def bindViewFromCacheOrItems(view: View, position: Int, entityData: AnyRef, parent: ViewGroup, crudContext: CrudContext, contextItems: GetterInput) {
    bindViewFromCacheOrItems(view, entityData, crudContext, contextItems, baseUriPath / getItemId(entityData, position), parent)
  }

  protected[scrud] def bindViewFromCacheOrItems(view: View, entityData: AnyRef, crudContext: CrudContext, contextItems: GetterInput, uriPath: UriPath, adapterView: ViewGroup) {
    view.setTag(uriPath)
    val application = crudContext.application
    val futurePortableValue = application.futurePortableValue(entityType, uriPath, entityData, crudContext)
    val updaterInput = UpdaterInput(view, contextItems)
    if (futurePortableValue.isSet) {
      futurePortableValue().update(updaterInput)
    } else {
      entityType.defaultValue.update(updaterInput)
      futurePortableValue.foreach { portableValue =>
        platformDriver.runOnUiThread(view) {
          if (view.getTag == uriPath) {
            portableValue.update(updaterInput)
          }
        }
      }
    }
  }
}

class AdapterCachingStateListener[A <: Adapter](adapterView: AdapterView[A], entityType: EntityType,
                                                platformDriver: AndroidPlatformDriver, crudContext: CrudContext, adapterFactory: => A) extends CachedStateListener with Logging {
  protected def logTag = entityType.logTag

  def onSaveState(outState: Bundle) {
  }

  def onRestoreState(savedInstanceState: Bundle) {
  }

  def onClearState(stayActive: Boolean) {
    crudContext.application.FuturePortableValueCache.get(crudContext).clear()
    if (stayActive) {
      adapterView.setAdapter(adapterFactory)
    }
  }
}
