package com.github.scrud.copy

import com.github.scrud.context.RequestContext

/**
 * A base class for an AdaptedField which doesn't know which value type it contains.
 * @author Eric Pabst (epabst@gmail.com)
 *         Date: 12/12/13
 *         Time: 3:08 PM
 */
abstract class BaseAdaptedField {
  def copyAndUpdate[T <: AnyRef](source: AnyRef, target: T, requestContext: RequestContext): T
}