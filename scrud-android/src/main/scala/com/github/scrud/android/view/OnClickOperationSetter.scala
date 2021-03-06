package com.github.scrud.android.view

import android.view.View
import com.github.triangle.{UpdaterInput, Setter, &&}
import com.github.scrud.android.view.AndroidConversions._
import com.github.scrud.{CrudContextField, UriField}
import com.github.scrud.action.Operation

/** A Setter that invokes an Operation when the View is clicked.
  * @author Eric Pabst (epabst@gmail.com)
  */
case class OnClickOperationSetter[T](viewOperation: View => Operation) extends Setter[T] {
  /**A setter.  It is identical to updater but doesn't have to return the modified subject. */
  def setter[S <: AnyRef]: PartialFunction[UpdaterInput[S,T],Unit] = {
    case UpdaterInput(view: View, _, CrudContextField(Some(crudContext)) && UriField(Some(uri))) =>
      if (view.isClickable) {
        view.setOnClickListener { view: View =>
          viewOperation(view).invoke(uri, crudContext)
        }
      }
  }
}
