package com.github.scrud.android.view

import com.xtremelabs.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.junit.Test
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import android.view.View
import com.github.scrud.android.action.{ActivityWithState, Operation}
import com.github.scrud.android.{CrudApplication, CrudContext}
import com.github.triangle.PortableField
import com.github.scrud.android.common.UriPath

/** A specification of [[com.github.scrud.android.view.OnClickOperationSetter]].
  * @author Eric Pabst (epabst@gmail.com)
  */
@RunWith(classOf[RobolectricTestRunner])
class OnClickOperationSetterSpec extends MockitoSugar {
  @Test
  def itMustSetOnClickListenerWhenClicableIsTrue() {
    val operation = mock[Operation]
    val view = mock[View]
    stub(view.isClickable).toReturn(true)
    val setter = OnClickOperationSetter[Unit](_ => operation)
    setter.setValue(view, None, List(UriPath.EMPTY, CrudContext(mock[MyActivityWithState], mock[CrudApplication]), PortableField.UseDefaults))
    verify(view).setOnClickListener(any())
  }

  @Test
  def itMustNotSetOnClickListenerWhenClickableIsFalse() {
    val operation = mock[Operation]
    val view = mock[View]
    stub(view.isClickable).toReturn(false)
    val setter = OnClickOperationSetter[Unit](_ => operation)
    setter.setValue(view, None, List(UriPath.EMPTY, CrudContext(mock[MyActivityWithState], mock[CrudApplication]), PortableField.UseDefaults))
    verify(view, never()).setOnClickListener(any())
  }
}

class MyActivityWithState extends ActivityWithState