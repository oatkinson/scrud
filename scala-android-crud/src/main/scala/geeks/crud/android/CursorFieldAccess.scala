package geeks.crud.android

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import geeks.crud._

object CursorFieldAccess {
  def persisted[T](name: String)(implicit persistedType: PersistedType[T]): CursorFieldAccess[T] = {
    new CursorFieldAccess[T](name)(persistedType)
  }

  def queryFieldNames(fields: List[CopyableField]): List[String] = {
    BaseColumns._ID :: fields.map(_.asInstanceOf[Field[_]].fieldAccesses).flatMap(_.flatMap(_ match {
      case fieldAccess: CursorFieldAccess[_] => Some(fieldAccess.name)
      case _ => None
    }))
  }

  def sqliteCriteria[T](name: String) = Field.writeOnly[SQLiteCriteria,T](criteria => value => criteria.selection = name + "=" + value)
}

/**
 * Also supports accessing a scala Map (mutable.Map for writing) using the same name.
 */
class CursorFieldAccess[T](val name: String)(implicit val persistedType: PersistedType[T]) extends FieldAccess[Cursor,ContentValues,T] {
  private lazy val mapAccess = Field.mapAccess[T](name)

  override def partialGet(readable: AnyRef) =
    super.partialGet(readable).orElse(mapAccess.partialGet(readable))

  override def partialSet(writable: AnyRef, value: T) =
    super.partialSet(writable, value) || mapAccess.partialSet(writable, value)

  def get(cursor: Cursor) = persistedType.getValue(cursor, cursor.getColumnIndex(name))

  def set(contentValues: ContentValues, value: T) = persistedType.putValue(contentValues, name, value)
}
