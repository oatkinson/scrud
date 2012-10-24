package com.github.scrud.android

import android.provider.BaseColumns
import android.database.Cursor
import android.content.ContentValues
import com.github.scrud.platform.PlatformTypes._
import persistence._
import scala.None
import collection.mutable
import android.app.backup.BackupManager
import android.database.sqlite.{SQLiteOpenHelper, SQLiteDatabase}
import com.github.triangle.{GetterInput, PortableField, Logging}
import com.github.scrud.{UriPath, EntityType}
import com.github.scrud.util.{Common, DelegatingListenerSet, ListenerSet}
import com.github.scrud.persistence.{CrudPersistence, DataListener}

/** EntityPersistence for SQLite.
  * @author Eric Pabst (epabst@gmail.com)
  */
class SQLiteEntityPersistence(val entityType: EntityType, val crudContext: CrudContext, databaseSetup: SQLiteOpenHelper,
                              protected val listenerSet: ListenerSet[DataListener])
  extends CrudPersistence with DelegatingListenerSet[DataListener] with Logging {

  lazy val tableName = SQLitePersistenceFactory.toTableName(entityType.entityName)
  lazy val database: SQLiteDatabase = databaseSetup.getWritableDatabase
  lazy val entityTypePersistedInfo = EntityTypePersistedInfo(entityType)
  private lazy val backupManager = new BackupManager(crudContext.activityContext)
  private lazy val deletedEntityIdCrudType = DeletedEntityIdCrudType
  private val cursors = new mutable.SynchronizedQueue[Cursor]
  private def toOption(string: String): Option[String] = if (string == "") None else Some(string)

  def findAll(criteria: SQLiteCriteria): CursorStream = {
    import entityTypePersistedInfo._
    val query = criteria.selection.mkString(" AND ")
    info("Finding each " + this.entityType.entityName + "'s " + queryFieldNames.mkString(", ") + " where " + query + criteria.orderBy.map(" order by " + _).getOrElse(""))
    val cursor = database.query(tableName, queryFieldNames.toArray,
      toOption(query).getOrElse(null), criteria.selectionArgs.toArray,
      criteria.groupBy.getOrElse(null), criteria.having.getOrElse(null), criteria.orderBy.getOrElse(null))
    cursors += cursor
    CursorStream(cursor, entityTypePersistedInfo)
  }

  //UseDefaults is provided here in the item list for the sake of PortableField.adjustment[SQLiteCriteria] fields
  def findAll(uri: UriPath): CursorStream =
    // The default orderBy is Some("_id desc")
    findAll(entityType.copyAndUpdate(GetterInput(uri, PortableField.UseDefaults), new SQLiteCriteria(orderBy = Some(CursorField.idFieldName + " desc"))))

  private def notifyDataChanged() {
    backupManager.dataChanged()
    debug("Notified BackupManager that data changed.")
  }

  def newWritable = SQLitePersistenceFactory.newWritable

  def doSave(idOption: Option[ID], writable: AnyRef): ID = {
    val contentValues = writable.asInstanceOf[ContentValues]
    val id = idOption match {
      case None => {
        val newId = database.insertOrThrow(tableName, null, contentValues)
        info("Added " + entityType.entityName + " #" + newId + " with " + contentValues)
        newId
      }
      case Some(givenId) => {
        info("Updating " + entityType.entityName + " #" + givenId + " with " + contentValues)
        val rowCount = database.update(tableName, contentValues, BaseColumns._ID + "=" + givenId, null)
        if (rowCount == 0) {
          contentValues.put(BaseColumns._ID, givenId)
          info("Added " + entityType.entityName + " #" + givenId + " with " + contentValues + " since id is not present yet")
          val resultingId = database.insert(tableName, null, contentValues)
          if (givenId != resultingId)
            throw new IllegalStateException("id changed from " + givenId + " to " + resultingId +
                    " when restoring " + entityType.entityName + " #" + givenId + " with " + contentValues)
        }
        givenId
      }
    }
    notifyDataChanged()
    val map = entityType.copyAndUpdate(contentValues, Map[String,Any]())
    val bytes = CrudBackupAgent.marshall(map)
    debug("Scheduled backup which will include " + entityType.entityName + "#" + id + ": size " + bytes.size + " bytes")
    id
  }

  def doDelete(uri: UriPath) {
    val ids = findAll(uri).map { readable =>
      val id = entityType.IdField.getRequired(readable)
      database.delete(tableName, BaseColumns._ID + "=" + id, Nil.toArray)
      id
    }
    future {
      ids.foreach { id =>
        deletedEntityIdCrudType.recordDeletion(entityType, id, crudContext.activityContext)
      }
      notifyDataChanged()
    }
  }

  def close() {
    cursors.map(_.close())
    database.close()
  }
}

class GeneratedDatabaseSetup(crudContext: CrudContext)
  extends SQLiteOpenHelper(crudContext.activityContext, crudContext.application.nameId, null, crudContext.application.dataVersion) with Logging {

  protected lazy val logTag = Common.tryToEvaluate(crudContext.application.logTag).getOrElse(Common.logTag)

  private def createMissingTables(db: SQLiteDatabase) {
    val application = crudContext.application
    application.allEntityTypes.filter(application.isSavable(_)).foreach { entityType =>
      val buffer = new StringBuffer
      buffer.append("CREATE TABLE IF NOT EXISTS ").append(SQLitePersistenceFactory.toTableName(entityType.entityName)).append(" (").
        append(BaseColumns._ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT")
      EntityTypePersistedInfo(entityType).persistedFields.filter(_.columnName != BaseColumns._ID).foreach { persisted =>
        buffer.append(", ").append(persisted.columnName).append(" ").append(persisted.persistedType.sqliteType)
      }
      buffer.append(")")
      execSQL(db, buffer.toString)
    }
  }

  def onCreate(db: SQLiteDatabase) {
    createMissingTables(db)
  }

  private def execSQL(db: SQLiteDatabase, sql: String) {
    debug("execSQL: " + sql)
    db.execSQL(sql)
  }

  def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    // Steps to upgrade the database for the new version ...
    createMissingTables(db)
  }
}
