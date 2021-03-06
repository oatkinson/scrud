package com.github.scrud.android.action

import org.junit.runner.RunWith
import org.junit.Test
import org.scalatest.matchers.MustMatchers
import android.app.Activity
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eql, _}
import android.view.{MenuItem, Menu}
import com.github.scrud.action.{CommandId, Command}
import com.github.scrud.android.CustomRobolectricTestRunner

/** A behavior specification for [[com.github.scrud.android.action.OptionsMenuActivity]].
  * @author Eric Pabst (epabst@gmail.com)
  */
@RunWith(classOf[CustomRobolectricTestRunner])
class OptionsMenuActivitySpec extends MustMatchers with MockitoSugar {
  class StubOptionsMenuActivity extends Activity with OptionsMenuActivity {
    protected val defaultOptionsMenuCommands = Nil

    protected val logTag = "log"
  }

  @Test
  def mustUseLatestOptionsMenuForCreate() {
    val activity = new StubOptionsMenuActivity
    activity.optionsMenuCommands = List(Command(CommandId("command1"), None, Some(10)))

    val menu = mock[Menu]
    val menuItem = mock[MenuItem]
    stub(menu.add(anyInt(), anyInt(), anyInt(), anyInt())).toReturn(menuItem)
    activity.onCreateOptionsMenu(menu)
    verify(menu, times(1)).add(anyInt(), eql(10), anyInt(), anyInt())
  }

  @Test
  def mustUseLatestOptionsMenuForPrepare_Android2() {
    val activity = new StubOptionsMenuActivity
    activity.optionsMenuCommands = List(Command(CommandId("command1"), None, Some(10)))

    val menu = mock[Menu]
    activity.onPrepareOptionsMenu(menu)
    verify(menu, times(1)).add(anyInt(), eql(10), anyInt(), anyInt())
  }

  @Test
  def mustCallInvalidateOptionsMenuAndNotRepopulateForAndroid3WhenSet() {
    val activity = new StubOptionsMenuActivity {
      var invalidated, populated = false

      //this will be called using reflection
      def invalidateOptionsMenu() {
        invalidated = true
      }

      override private[action] def populateMenu(menu: Menu, actions: List[Command]) {
        populated = true
      }
    }
    activity.optionsMenuCommands = List(mock[Command])
    activity.invalidated must be (true)
    activity.populated must be (false)

    val menu = mock[Menu]
    activity.onPrepareOptionsMenu(menu)
    activity.populated must be (false)
    verify(menu, never()).clear()
  }

  @Test
  def mustNotRepopulateInPrepareWhenNotSet_Android3() {
    val activity = new StubOptionsMenuActivity {
      var populated = false

      def invalidateOptionsMenu() {}

      override private[action] def populateMenu(menu: Menu, actions: List[Command]) {
        populated = true
      }
    }
    val menu = mock[Menu]
    activity.onPrepareOptionsMenu(menu)
    verify(menu, never()).clear()
    activity.populated must be (false)
  }

  @Test
  def mustNotRepopulateInPrepareWhenNotSet_Android2() {
    val activity = new StubOptionsMenuActivity {
      var populated = false

      override private[action] def populateMenu(menu: Menu, actions: List[Command]) {
        populated = true
      }
    }
    val menu = mock[Menu]
    activity.onPrepareOptionsMenu(menu)
    verify(menu, never()).clear()
    activity.populated must be (false)
  }

  @Test
  def mustRepopulateInPrepareForAndroid2AfterSetting() {
    val activity = new StubOptionsMenuActivity {
      var populated = false

      override private[action] def populateMenu(menu: Menu, actions: List[Command]) {
        populated = true
      }
    }
    activity.optionsMenuCommands = List(mock[Command])
    activity.populated must be (false)

    val menu = mock[Menu]
    activity.onPrepareOptionsMenu(menu)
    verify(menu).clear()
    activity.populated must be (true)
  }

  @Test
  def mustOnlyRepopulateOnceForAndroid2AfterSetting() {
    val activity = new StubOptionsMenuActivity {
      var populated = 0

      override private[action] def populateMenu(menu: Menu, actions: List[Command]) {
        populated += 1
      }
    }
    activity.optionsMenuCommands = List(mock[Command])
    activity.populated must be (0)

    val menu = mock[Menu]
    activity.onPrepareOptionsMenu(menu)
    activity.onPrepareOptionsMenu(menu)
    activity.populated must be (1)
  }
}
