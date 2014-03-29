package com.github.scrud.action

import com.github.scrud.util.Name
import com.github.scrud.action.RestMethod.RestMethod

/**
 * The unique identifier for a Command.
 * The intent may depend heavily on the Uri it is used with.
 * The string is not intended for display.  It should be localized to a display string.
 * @param name a name such as a Java identifier like PoliceOfficer (pascal case) or crescentWrench (camel case).
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 12/20/12
 * Time: 6:20 PM
 * @see [[com.github.scrud.action.Action]]
 */
case class CommandKey(name: String, restMethodOpt: Option[RestMethod] = None) extends Name

/**
 * This contains a set of command [[com.github.scrud.action.CommandKey]]s.
 * Others may be created as needed by specific applications.
 */
object CommandKey {
  /**
   * The command to allow the user to fill in the data to add an entity.
   * Normally, the UI will render this as an "Add" button or a "+" button.
   * This represents part of the "C" in CRUD.
   */
  object Add extends CommandKey("Add")

  /**
   * The command to actually create/save a new entity.  The new Uri will be returned.
   * This is often after an [[com.github.scrud.action.CommandKey.Add]] command.
   * Normally, the UI will render this as a "Create" button or "Save" button.
   * This represents part of the "C" in CRUD.
   * This corresponds to a POST in REST.
   */
  object Create extends CommandKey("Create", Some(RestMethod.POST))

  /**
   * The command to get data for a Uri, whether a single entity or a list of entities.
   * Normally, the UI will render this as clickable text.
   * This represents the "R" in CRUD.
   * This corresponds to a GET in REST.
   */
  object View extends CommandKey("View", Some(RestMethod.GET))

  /**
   * The command to let the user modify the data for an entity.
   * Normally, the UI will render this is an "Edit" button or a pencil icon.
   * This represents part of the "U" in CRUD.
   */
  object Edit extends CommandKey("Edit")

  /**
   * The command to save an entity with a given Uri.
   * Normally, the UI will render this is a "Save" button.
   * This represents part of the "U" in CRUD.
   * This corresponds to a PUT in REST.
   */
  object Save extends CommandKey("Save", Some(RestMethod.PUT))

  /**
   * The command to delete an entity.
   * Normally, the UI will render this is a "Delete" button or red "X" icon.
   * This represents the "D" in CRUD.
   * This corresponds to a DELETE in REST.
   */
  object Delete extends CommandKey("Delete", Some(RestMethod.DELETE))
}
