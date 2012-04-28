package com.github.scala.android.crud.view

import android.view.View
import com.github.scala.android.crud.action.{ActivityWithState, Operation}
import com.github.triangle.{SetterUsingItems,&&}
import com.github.scala.android.crud.view.AndroidConversions._
import com.github.scala.android.crud.{UriField, CrudContextField, CrudContext}

/** A Setter that invokes an Operation when the View is clicked.
  * @author Eric Pabst (epabst@gmail.com)
  */
case class OnClickOperationSetter[T](viewOperation: View => Operation) extends SetterUsingItems[T]  {
  override def setterUsingItems = {
    case (view: View, CrudContextField(Some(CrudContext(activity: ActivityWithState, _))) && UriField(Some(uri))) => ignoredValue => {
      if (view.isClickable) {
        view.setOnClickListener { view: View =>
          viewOperation(view).invoke(uri, activity)
        }
      }
    }
  }
}
