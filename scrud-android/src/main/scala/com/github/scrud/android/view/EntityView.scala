package com.github.scrud.android.view

import com.github.scrud.android.persistence.EntityType
import com.github.scrud.android.common.PlatformTypes.ID
import com.github.triangle.PortableField._
import com.github.triangle.{GetterInput, SetterUsingItems, Getter, &&}
import android.widget._
import android.view.View
import android.app.Activity
import xml.NodeSeq
import com.github.scrud.android.{CrudContextField, UriField, BaseCrudActivity, CrudContext}

/** A ViewField that allows choosing a specific entity of a given EntityType or displaying its fields' values.
  * The layout for the EntityType that contains this EntityView may refer to fields of this view's EntityType
  * in the same way as referring to its own fields.  If both have a field of the same name, the behavior is undefined.
  * @author Eric Pabst (epabst@gmail.com)
  */
case class EntityView(entityType: EntityType)
  extends ViewField[ID](FieldLayout(displayXml = NodeSeq.Empty, editXml = <Spinner android:drawSelectorOnTop = "true"/>)) {

  private object AndroidUIElement {
    def unapply(target: AnyRef): Option[AnyRef] = target match {
      case view: View => Some(view)
      case activity: Activity => Some(activity)
      case _ => None
    }
  }

  protected val delegate = Getter[AdapterView[BaseAdapter], ID](v => Option(v.getSelectedItemId)) + SetterUsingItems[ID] {
    case (adapterView: AdapterView[BaseAdapter], UriField(Some(uri)) && CrudContextField(Some(crudContext @ CrudContext(crudActivity: BaseCrudActivity, _)))) => idOpt: Option[ID] =>
      if (idOpt.isDefined || adapterView.getAdapter == null) {
        val crudType = crudContext.application.crudType(entityType)
        //don't do it again if already done from a previous time
        if (adapterView.getAdapter == null) {
          crudType.setListAdapter(adapterView, entityType, uri, crudContext, crudActivity.contextItems, crudActivity,
            crudActivity.pickLayout(entityType))
        }
        if (idOpt.isDefined) {
          val adapter = adapterView.getAdapter
          val position = (0 to (adapter.getCount - 1)).view.map(adapter.getItemId(_)).indexOf(idOpt.get)
          adapterView.setSelection(position)
        }
      }
    case (AndroidUIElement(uiElement), input @ UriField(Some(baseUri)) && CrudContextField(Some(crudContext))) => idOpt: Option[ID] =>
      val entityOpt = idOpt.flatMap(id => crudContext.withEntityPersistence(entityType)(_.find(id, baseUri)))
      entityType.copy(GetterInput(entityOpt.getOrElse(UseDefaults) +: input.items), uiElement)
  }

  override def toString = "EntityView(" + entityType.entityName + ")"
}
