package com.github.scrud

import com.github.scrud.platform.PlatformTypes._
import com.github.scrud.platform.IdFormat

/** A convenience wrapper for UriPath.
  * It helps in that UriPath.EMPTY is null when running unit tests, and helps prepare for multi-platform support.
  * @author Eric Pabst (epabst@gmail.com)
  */
case class UriPath(segments: String*) {
  private lazy val idFormat = IdFormat

  def /(segment: String): UriPath = UriPath(segments :+ segment:_*)

  def /(entityName: EntityName): UriPath = this / entityName.name

  def /(id: ID): UriPath = this / idFormat.toString(id)

  def specify(finalSegments: String*): UriPath =
    UriPath.replacePathSegments(this, _.takeWhile(_ != finalSegments.head) ++ finalSegments.toList)

  def specify(entityName: EntityName): UriPath = specify(entityName.name)

  def specify(entityName: EntityName, id: ID): UriPath = specify(entityName.name, id.toString)

  def specifyLastEntityName(entityName: EntityName): UriPath =
    specify(entityName.name +: findId(entityName).map(_.toString).toList:_*)

  lazy val lastEntityNameOption: Option[EntityName] = segments.reverse.find(idFormat.toValue(_).isFailure).map(EntityName(_))

  def lastEntityNameOrFail: EntityName = lastEntityNameOption.getOrElse {
    throw new IllegalArgumentException("an EntityName must be specified in the URI but uri=" + this)
  }

  def findId(entityName: EntityName): Option[ID] =
    segments.dropWhile(_ != entityName.name).toList match {
      case _ :: idString :: tail => idFormat.toValue(idString).toOption
      case _ => None
    }

  override lazy val toString = segments.mkString("/", "/", "")
}

object UriPath {
  val EMPTY: UriPath = UriPath()

  private def toOption(string: String): Option[String] = if (string == "") None else Some(string)

  def apply(string: String): UriPath = UriPath(toOption(string.stripPrefix("/")).map(_.split("/").toSeq).getOrElse(Nil):_*)

  def apply(entityName: EntityName): UriPath = UriPath(entityName.name)

  def apply(entityName: EntityName, id: ID): UriPath = UriPath(entityName.name, id.toString)

  private[UriPath] def replacePathSegments(uri: UriPath, f: Seq[String] => Seq[String]): UriPath = {
    val path = f(uri.segments)
    UriPath(path: _*)
  }
}
