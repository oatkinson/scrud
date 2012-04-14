package com.github.scala.android.crud

import action._
import common.{UriPath, Timing}
import Operation._
import android.app.Activity
import com.github.triangle._
import common.PlatformTypes._
import persistence.{EntityTypePersistedInfo, CursorStream, EntityType, PersistenceListener}
import PortableField.toSome
import view.AndroidResourceAnalyzer._
import java.lang.IllegalStateException
import android.view.View
import android.content.Context
import android.database.Cursor
import android.widget._
import view.{ViewRef, AdapterCachingStateListener, AdapterCaching, EntityAdapter}

/** An entity configuration that provides all custom information needed to
  * implement CRUD on the entity.  This shouldn't depend on the platform (e.g. android).
  * @author Eric Pabst (epabst@gmail.com)
  */
class CrudType(val entityType: EntityType, val persistenceFactory: PersistenceFactory) extends Timing with Logging { self =>
  protected def logTag = entityType.logTag

  trace("Instantiated CrudType: " + this)

  def entityName = entityType.entityName
  lazy val entityNameLayoutPrefix = NamingConventions.toLayoutPrefix(entityName)

  def rLayoutClasses: Seq[Class[_]] = detectRLayoutClasses(this.getClass)
  private lazy val rLayoutClassesVal = rLayoutClasses
  def rStringClasses: Seq[Class[_]] = detectRStringClasses(this.getClass)
  private lazy val rStringClassesVal = rStringClasses
  def rIdClasses: Seq[Class[_]] = detectRIdClasses(this.getClass)
  private lazy val rIdClassesVal = rIdClasses

  protected def getLayoutKey(layoutName: String): LayoutKey =
    findResourceIdWithName(rLayoutClassesVal, layoutName).getOrElse {
      rLayoutClassesVal.foreach(layoutClass => logError("Contents of " + layoutClass + " are " + layoutClass.getFields.mkString(", ")))
      throw new IllegalStateException("R.layout." + layoutName + " not found.  You may want to run the CrudUIGenerator.generateLayouts." +
              rLayoutClassesVal.mkString("(layout classes: ", ",", ")"))
    }

  lazy val headerLayout: LayoutKey = getLayoutKey(entityNameLayoutPrefix + "_header")
  lazy val listLayout: LayoutKey =
    findResourceIdWithName(rLayoutClassesVal, entityNameLayoutPrefix + "_list").getOrElse(getLayoutKey("entity_list"))
  lazy val rowLayout: LayoutKey = getLayoutKey(entityNameLayoutPrefix + "_row")
  lazy val displayLayout: Option[LayoutKey] = findResourceIdWithName(rLayoutClassesVal, entityNameLayoutPrefix + "_display")
  /** The layout used for each entity when allowing the user to pick one of them. */
  lazy val pickLayout: LayoutKey = findResourceIdWithName(rLayoutClassesVal, entityNameLayoutPrefix + "_pick").getOrElse(
    _root_.android.R.layout.simple_spinner_dropdown_item)
  lazy val entryLayout: LayoutKey = getLayoutKey(entityNameLayoutPrefix + "_entry")

  final def hasDisplayPage = displayLayout.isDefined

  /** This uses isDeletable because it assumes that if it can be deleted, it can be added as well.
    * @see [[com.github.scala.android.crud.CrudType.isDeletable]].
    */
  lazy val isAddable: Boolean = isDeletable

  /** @see [[com.github.scala.android.crud.PersistenceFactory.canDelete]]. */
  final lazy val isDeletable: Boolean = persistenceFactory.canDelete

  /** @see [[com.github.scala.android.crud.PersistenceFactory.canSave]]. */
  final lazy val isUpdateable: Boolean = persistenceFactory.canSave

  lazy val entityTypePersistedInfo = EntityTypePersistedInfo(entityType)

  protected def getStringKey(stringName: String): SKey =
    findResourceIdWithName(rStringClassesVal, stringName).getOrElse {
      rStringClassesVal.foreach(rStringClass => logError("Contents of " + rStringClass + " are " + rStringClass.getFields.mkString(", ")))
      throw new IllegalStateException("R.string." + stringName + " not found.  You may want to run the CrudUIGenerator.generateLayouts." +
              rStringClassesVal.mkString("(string classes: ", ",", ")"))
    }

  def commandToListItems: Command = Command(None,
    findResourceIdWithName(rStringClassesVal, entityNameLayoutPrefix + "_list"), None)

  def commandToDisplayItem: Command = Command(None, None, None)

  def commandToAddItem: Command = Command(android.R.drawable.ic_menu_add,
    getStringKey("add_" + entityNameLayoutPrefix),
    Some(ViewRef("add_" + entityNameLayoutPrefix + "_command", rIdClassesVal)))

  def commandToEditItem: Command = Command(android.R.drawable.ic_menu_edit,
    getStringKey("edit_" + entityNameLayoutPrefix), None)

  def commandToDeleteItem: Command = Command(android.R.drawable.ic_menu_delete, res.R.string.delete_item, None)

  lazy val commandToUndoDelete = Command(None, Some(res.R.string.undo_delete), None)

  lazy val parentFields: List[ParentField] = entityType.deepCollect {
    case parentField: ParentField => parentField
  }

  def parentEntityTypes(application: CrudApplication): List[EntityType] = parentFields.map(_.entityType)

  def childEntityTypes(application: CrudApplication): List[EntityType] = childEntities(application).map(_.entityType)

  /** The list of entities that refer to this one.
    * Those entities should have a ParentField (or foreignKey) in their fields list.
    */
  def childEntities(application: CrudApplication): List[CrudType] = {
    trace("childEntities: allCrudTypes=" + application.allEntityTypes + " self=" + self)
    application.allCrudTypes.filter { entity =>
      val parentEntityTypes = entity.parentEntityTypes(application)
      trace("childEntities: parents of " + entity.entityType + " are " + parentEntityTypes)
      parentEntityTypes.contains(self.entityType)
    }
  }

  /** Gets the action to display a UI for a user to fill in data for creating an entity.
    * The target Activity should copy Unit into the UI using entityType.copy to populate defaults.
    */
  @deprecated("use CrudApplication.actionToCreate")
  lazy val createAction: Option[Action] =
    if (isAddable)
      Some(Action(commandToAddItem, new StartEntityActivityOperation(entityType.entityName, CreateActionName, activityClass)))
    else
      None

  /** Gets the action to display the list that matches the criteria copied from criteriaSource using entityType.copy. */
  @deprecated("use CrudApplication.actionToList.get")
  lazy val listAction = Action(commandToListItems, new StartEntityActivityOperation(entityType.entityName, ListActionName, listActivityClass))

  protected def entityOperation(action: String, activityClass: Class[_ <: Activity]) =
    new StartEntityIdActivityOperation(entityType.entityName, action, activityClass)

  /** Gets the action to display the entity given the id in the UriPath. */
  @deprecated("use CrudApplication.actionToDisplay.get")
  lazy val displayAction = Action(commandToDisplayItem, entityOperation(DisplayActionName, activityClass))

  /** Gets the action to display a UI for a user to edit data for an entity given its id in the UriPath. */
  @deprecated("use CrudApplication.actionToUpdate")
  lazy val updateAction: Option[Action] =
    if (isUpdateable) Some(Action(commandToEditItem, entityOperation(UpdateActionName, activityClass)))
    else None

  @deprecated("use CrudApplication.actionToDelete")
  lazy val deleteAction: Option[Action] =
    if (isDeletable) {
      Some(Action(commandToDeleteItem, new Operation {
        def invoke(uri: UriPath, activity: ActivityWithState) {
          activity match {
            case crudActivity: BaseCrudActivity => startDelete(uri, crudActivity)
          }
        }
      }))
    } else None


  def listActivityClass: Class[_ <: CrudListActivity] = classOf[CrudListActivity]
  def activityClass: Class[_ <: CrudActivity] = classOf[CrudActivity]

  /** Gets the actions that a user can perform from a list of the entities.
    * May be overridden to modify the list of actions.
    */
  @deprecated("use CrudApplication.actionsForList")
  def getListActions(application: CrudApplication): List[Action] =
    getReadOnlyListActions(application) ::: application.actionToCreate(entityType).toList

  protected def getReadOnlyListActions(application: CrudApplication): List[Action] = {
    val thisEntity = this.entityType;
    (parentFields match {
      //exactly one parent w/o a display page
      case parentField :: Nil if !application.crudType(parentField.entityType).hasDisplayPage => {
        val parentEntityType = parentField.entityType
        //the parent's actionToUpdate should be shown since clicking on the parent entity brought the user
        //to the list of child entities instead of to a display page for the parent entity.
        application.actionToUpdate(parentEntityType).toList :::
          application.childEntityTypes(parentEntityType).filter(_ != thisEntity).flatMap(application.actionToList(_))
      }
      case _ => Nil
    })
  }

  /** Gets the actions that a user can perform from a specific entity instance.
    * The first one is the one that will be used when the item is clicked on.
    * May be overridden to modify the list of actions.
    */
  @deprecated("use CrudApplication.actionsForEntity")
  def getEntityActions(application: CrudApplication): List[Action] =
    getReadOnlyEntityActions(application) ::: application.actionToUpdate(entityType).toList :::
      application.actionToDelete(entityType).toList

  protected def getReadOnlyEntityActions(application: CrudApplication): List[Action] =
    displayLayout.flatMap(_ => application.actionToDisplay(entityType)).toList :::
      application.childEntityTypes(entityType).flatMap(application.actionToList(_))

  def addPersistenceListener(listener: PersistenceListener, crudContext: CrudContext) {
    persistenceFactory.addListener(listener, entityType, crudContext)
  }

  def openEntityPersistence(crudContext: CrudContext): CrudPersistence = createEntityPersistence(crudContext)

  /** Instantiates a data buffer which can be saved by EntityPersistence.
    * The fields must support copying into this object.
    */
  def newWritable = persistenceFactory.newWritable

  protected def createEntityPersistence(crudContext: CrudContext) = persistenceFactory.createEntityPersistence(entityType, crudContext)

  final def withEntityPersistence[T](crudContext: CrudContext)(f: CrudPersistence => T): T = {
    val persistence = openEntityPersistence(crudContext)
    try f(persistence)
    finally persistence.close()
  }

  final def setListAdapterUsingUri(crudContext: CrudContext, activity: CrudListActivity) {
    setListAdapter(activity.getListView, entityType, activity.currentUriPath, crudContext, activity.contextItems, activity, self.rowLayout)
  }

  private def createAdapter[A <: Adapter](persistence: CrudPersistence, uriPath: UriPath, entityType: EntityType, crudContext: CrudContext, contextItems: scala.List[AnyRef], activity: Activity, itemLayout: LayoutKey, adapterView: AdapterView[A]): AdapterCaching = {
    val findAllResult = persistence.findAll(uriPath)
    findAllResult match {
      case CursorStream(cursor, _) =>
        activity.startManagingCursor(cursor)
        addPersistenceListener(new PersistenceListener {
          def onSave(id: ID) {
            cursor.requery()
          }
          def onDelete(uri: UriPath) {
            cursor.requery()
          }
        }, crudContext)
        new ResourceCursorAdapter(activity, itemLayout, cursor) with AdapterCaching {
          def entityType = self.entityType

          def bindView(view: View, context: Context, cursor: Cursor) {
            val row = entityTypePersistedInfo.copyRowToMap(cursor)
            bindViewFromCacheOrItems(view, row, contextItems, cursor.getPosition, adapterView)
          }
        }
      case _ => new EntityAdapter(entityType, findAllResult, itemLayout, contextItems, activity.getLayoutInflater)
    }
  }

  private def setListAdapter[A <: Adapter](adapterView: AdapterView[A], persistence: CrudPersistence, uriPath: UriPath, entityType: EntityType, crudContext: CrudContext, contextItems: scala.List[AnyRef], activity: Activity, itemLayout: LayoutKey) {
    addPersistenceListener(new PersistenceListener {
      def onSave(id: ID) {
        AdapterCaching.clearCache(adapterView, "save")
      }

      def onDelete(uri: UriPath) {
        trace("Clearing cache in " + adapterView + " of " + entityType + " due to delete")
        AdapterCaching.clearCache(adapterView, "delete")
      }
    }, crudContext)
    def callCreateAdapter(): A = {
      createAdapter(persistence, uriPath, entityType, crudContext, contextItems, activity, itemLayout, adapterView).asInstanceOf[A]
    }
    val adapter = callCreateAdapter()
    adapterView.setAdapter(adapter)
    crudContext.addCachedActivityStateListener(new AdapterCachingStateListener(adapterView, entityType, adapterFactory = callCreateAdapter()))
  }

  def setListAdapter[A <: Adapter](adapterView: AdapterView[A], entityType: EntityType, uriPath: UriPath, crudContext: CrudContext, contextItems: scala.List[AnyRef], activity: Activity, itemLayout: LayoutKey) {
    val persistence = crudContext.openEntityPersistence(entityType)
    crudContext.activityState.addListener(new DestroyStateListener {
      def onDestroyState() {
        persistence.close()
      }
    })
    setListAdapter(adapterView, persistence, uriPath, entityType, crudContext, contextItems, activity, itemLayout)
  }

  private[crud] def undoableDelete(uri: UriPath)(persistence: CrudPersistence) {
    persistence.find(uri).foreach { readable =>
      val id = entityType.IdField.getter(readable)
      val writable = entityType.copyAndTransform(readable, newWritable)
      persistence.delete(uri)
      val undoDeleteOperation = new PersistenceOperation(entityType, persistence.crudContext.application) {
        def invoke(uri: UriPath, persistence: CrudPersistence) {
          persistence.save(id, writable)
        }
      }
      //todo delete childEntities recursively
      val context = persistence.crudContext.activityContext
      context match {
        case activity: BaseCrudActivity =>
          activity.allowUndo(Undoable(Action(commandToUndoDelete, undoDeleteOperation), None))
        case _ =>
      }
    }
  }

  /** Delete an entity by Uri with an undo option.  It can be overridden to do a confirmation box if desired. */
  def startDelete(uri: UriPath, activity: BaseCrudActivity) {
    withEntityPersistence(activity.crudContext)(undoableDelete(uri))
  }
}

/** An undo of an operation.  The operation should have already completed, but it can be undone or accepted.
  * @param undoAction  An Action that reverses the operation.
  * @param closeOperation  An operation that releases any resources, and is guaranteed to be called.
  *           For example, deleting related entities if undo was not called.
  */
case class Undoable(undoAction: Action, closeOperation: Option[Operation] = None)
