package com.github.scrud.android.view

import android.widget.BaseAdapter
import com.github.scrud.android.common.PlatformTypes._
import scala.Predef._
import com.github.scrud.android.persistence.EntityType
import android.view.{LayoutInflater, ViewGroup, View}
import com.github.triangle.GetterInput
import com.github.scrud.android.AndroidPlatformDriver

/** An Android Adapter for an EntityType with the result of EntityPersistence.findAll.
  * @author Eric Pabst (epabst@gmail.com)
  */
class EntityAdapter(val entityType: EntityType, values: Seq[AnyRef], rowLayout: ViewKey,
                    contextItems: GetterInput, val platformDriver: AndroidPlatformDriver, layoutInflater: LayoutInflater) extends BaseAdapter with AdapterCaching {
  def getCount: Int = values.size

  def getItemId(position: Int): ID = getItem(position) match {
    case entityType.IdField(Some(id)) => id
    case _ => position
  }

  def getItem(position: Int) = values(position)

  def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    val view = if (convertView == null) layoutInflater.inflate(rowLayout, parent, false) else convertView
    bindViewFromCacheOrItems(view, getItem(position), contextItems, position, parent)
    view
  }
}
