package com.github.scrud

import com.github.triangle.{Updater, UpdaterInput}
import scala.{PartialFunction, AnyRef}

/** A PortableField for validating data.  It updates a ValidationResult using a value.
  * @author Eric Pabst (epabst@gmail.com)
  */
class Validation[T](isValid: Option[T] => Boolean) extends Updater[T] {
  def updater[S <: AnyRef]: PartialFunction[UpdaterInput[S,T],S] = {
    case UpdaterInput(result: ValidationResult, valueOpt, _) => (result + isValid(valueOpt)).asInstanceOf[S]
  }
}

object Validation {
  def apply[T](isValid: Option[T] => Boolean): Validation[T] = new Validation[T](isValid)

  /** A Validation that requires that the value be defined.
    * It does allow the value to be an empty string, empty list, etc.
    * Example: <pre>field... + required</pre>
    */
  def required[T]: Validation[T] = Validation(_.isDefined)

  /** A Validation that requires that the value be defined and not one of the given values.
    * Example: <pre>field... + requiredAndNot("")</pre>
    */
  def requiredAndNot[T](invalidValues: T*): Validation[T] =
    Validation(value => value.isDefined && !invalidValues.contains(value.get))

  /** A Validation that requires that the value not be empty (after trimming). */
  lazy val requiredString: Validation[String] = Validation(_.map(s => s.trim != "").getOrElse(false))
}

case class ValidationResult(numInvalid: Int) {
  val isValid: Boolean = numInvalid == 0

  def +(isValid: Boolean): ValidationResult = if (isValid) this else ValidationResult(numInvalid + 1)
}

object ValidationResult {
  /** The result for valid data.  It is capitalized so it can be used in case statements. */
  val Valid: ValidationResult = ValidationResult(0)
}
