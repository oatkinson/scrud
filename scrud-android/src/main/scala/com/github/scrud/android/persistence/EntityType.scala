package com.github.scrud.android.persistence

import com.github.triangle._
import com.github.scrud.android.common._
import PlatformTypes._
import CursorField.PersistedId
import com.github.scrud.LoadingIndicator
import com.github.scrud.android.entity.EntityName
import com.github.scrud.android.ParentField

/** An entity configuration that provides information needed to map data to and from persistence.
  * This shouldn't depend on the platform (e.g. android).
  * @author Eric Pabst (epabst@gmail.com)
  * @param entityName  this is used to identify the EntityType and for internationalized strings
  */
abstract class EntityType(val entityName: EntityName) extends FieldList with Logging {
  override lazy val logTag = Common.tryToEvaluate(entityName.name).getOrElse(Common.logTag)

  def UriPathId = entityName.UriPathId

  /** This should only be used in order to override this.  IdField should be used instead of this.
    * A field that uses IdPk.id is NOT included here because it could match a related entity that also extends IdPk,
    * which results in many problems.
    */
  protected def idField: PortableField[ID] = UriPathId + PersistedId
  object IdField extends Field[ID](idField)

  /** The fields other than the primary key. */
  def valueFields: List[BaseField]

  /** The idField along with accessors for IdPk instances. */
  lazy val idPkField = IdField + Getter[IdPk,ID](_.id).withUpdater(e => e.id(_)) +
    Setter((e: MutableIdPk) => e.id = _)
  lazy val fieldsIncludingIdPk = FieldList((idPkField +: fields): _*)

  /** These are all of the entity's fields, which includes IdPk.idField and the valueFields. */
  final lazy val fields: List[BaseField] = IdField +: valueFields

  lazy val parentFields: Seq[ParentField] = deepCollect {
    case parentField: ParentField => parentField
  }

  def parentEntityNames: Seq[EntityName] = parentFields.map(_.entityName)

  def toUri(id: ID) = UriPath(entityName, id)

  lazy val loadingValue: PortableValue = copyFrom(LoadingIndicator)

  override def toString() = entityName.toString
}
