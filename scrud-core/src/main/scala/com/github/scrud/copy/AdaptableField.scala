package com.github.scrud.copy


/**
 * A set of [[com.github.scrud.copy.TargetField]]s by [[com.github.scrud.copy.TargetType]]
 * and [[com.github.scrud.copy.TypedSourceField]]s by [[com.github.scrud.copy.SourceType]].
 * @author Eric Pabst (epabst@gmail.com)
 *         Date: 12/11/13
 *         Time: 9:36 AM
 */
abstract class AdaptableField[V] extends BaseAdaptableField { self =>
  def attemptToAdapt(sourceType: SourceType, targetType: TargetType): Option[AdaptedField[V]] = {
    for {
      sourceField <- findSourceField(sourceType)
      targetField <- findTargetField(targetType)
    } yield AdaptedField(sourceField, targetField)
  }

  def findSourceField(sourceType: SourceType): Option[SourceField[V]]

  def findTargetField(targetType: TargetType): Option[TargetField[V]]
}

object AdaptableField {
  def apply[V](sourceFields: Map[SourceType,SourceField[V]], targetFields: Map[TargetType,TargetField[V]]) =
    new AdaptableFieldByType[V](sourceFields, targetFields)

  private val Empty = new ExtensibleAdaptableField[Any] {
    def findSourceField(sourceType: SourceType): Option[Nothing] = None

    def findTargetField(targetType: TargetType): Option[Nothing] = None

    override def orElse(adaptableField: AdaptableField[Any]) = adaptableField match {
      case extensible: ExtensibleAdaptableField[Any] => extensible
      case _ => super.orElse(adaptableField)
    }
  }

  def empty[V] = Empty.asInstanceOf[ExtensibleAdaptableField[V]]
}
