package com.github.scala.android.crud

import _root_.android.content.Intent
import org.junit.Test
import org.junit.runner.RunWith
import com.xtremelabs.robolectric.RobolectricTestRunner
import com.xtremelabs.robolectric.tester.android.view.TestMenu
import org.scalatest.matchers.MustMatchers
import android.view.{View, ContextMenu}
import org.mockito.Mockito._
import org.mockito.Matchers._
import persistence.EntityType

/** A test for [[com.github.scala.android.crud.CrudListActivity]].
  * @author Eric Pabst (epabst@gmail.com)
  */
@RunWith(classOf[RobolectricTestRunner])
class CrudListActivitySpec extends MustMatchers with CrudMockitoSugar {
  @Test
  def mustNotCopyFromParentEntityIfUriPathIsInsufficient() {
    val crudType = mock[CrudType]
    val parentCrudType = mock[CrudType]
    val application = mock[CrudApplication]
    val entityType = mock[EntityType]
    stub(crudType.entityType).toReturn(entityType)
    stub(crudType.parentEntities(application)).toReturn(List(parentCrudType))
    stub(crudType.maySpecifyEntityInstance(any())).toReturn(false)

    val activity = new CrudListActivity(crudType, application)
    activity.populateFromParentEntities()
    verify(crudType, never()).copyFromPersistedEntity(any(), any())
  }

  @Test
  def shouldAllowAdding() {
    val application = mock[CrudApplication]
    val persistence = mock[CrudPersistence]
    val entityType = new MyEntityType
    val crudType = new MyCrudType(entityType, persistence)
    stub(application.crudType(entityType)).toReturn(crudType)
    when(persistence.findAll(any())).thenReturn(Seq(Map[String,Any]("name" -> "Bob", "age" -> 25)))
    val activity = new CrudListActivity(crudType, application)
    activity.setIntent(new Intent(Intent.ACTION_MAIN))
    activity.onCreate(null)
    val menu = new TestMenu(activity)
    activity.onCreateOptionsMenu(menu)
    val item0 = menu.getItem(0)
    item0.getTitle.toString must be ("Add")
    menu.size must be (1)

    activity.onOptionsItemSelected(item0) must be (true)
  }

  @Test
  def resultOfGetListAdapterMustBeEqualToGetListViewAndThenGetAdapter() {
    val application = mock[CrudApplication]
    stub(application.allCrudTypes).toReturn(Nil)
    val crudType = MyCrudType
    val activity = new CrudListActivity(crudType, application)
    activity.setContentView(crudType.listLayout)
    activity.getListView.getAdapter must be (activity.getListAdapter)
  }

  @Test
  def shouldHaveCorrectContextMenu() {
    val application = mock[CrudApplication]
    val contextMenu = mock[ContextMenu]
    val ignoredView: View = null
    val ignoredMenuInfo: ContextMenu.ContextMenuInfo = null
    stub(application.allCrudTypes).toReturn(Nil)
    val crudType = MyCrudType
    val activity = new CrudListActivity(crudType, application)
    activity.onCreateContextMenu(contextMenu, ignoredView, ignoredMenuInfo)
    verify(contextMenu).add(0, res.R.string.delete_item, 0, res.R.string.delete_item)
  }

  @Test
  def shouldHandleNoEntityOptions() {
    val application = mock[CrudApplication]
    val contextMenu = mock[ContextMenu]
    val ignoredView: View = null
    val ignoredMenuInfo: ContextMenu.ContextMenuInfo = null

    val crudType = new MyCrudType(new MyEntityType) {
      override def getEntityActions(application: CrudApplication) = Nil
    }
    val activity = new CrudListActivity(crudType, application)
    //shouldn't do anything
    activity.onCreateContextMenu(contextMenu, ignoredView, ignoredMenuInfo)
  }

  @Test
  def shouldRefreshOnResume() {
    val persistenceFactory = mock[PersistenceFactory]
    val persistence = mock[CrudPersistence]
    stub(persistenceFactory.createEntityPersistence(anyObject(), anyObject())).toReturn(persistence)
    when(persistence.findAll(any())).thenReturn(Seq(Map[String,Any]("name" -> "Bob", "age" -> 25)))
    val application = mock[CrudApplication]
    val entityType = new MyEntityType
    val crudType = new MyCrudType(entityType, persistenceFactory)
    stub(application.crudType(entityType)).toReturn(crudType)
    class MyCrudListActivity extends CrudListActivity(crudType, application) {
      //make it public for testing
      override def onPause() {
        super.onPause()
      }

      //make it public for testing
      override def onResume() {
        super.onResume()
      }
    }
    val activity = new MyCrudListActivity
    activity.setIntent(new Intent(Intent.ACTION_MAIN))
    activity.onCreate(null)
    activity.onPause()
    //verify(persistenceFactory, never()).refreshAfterDataChanged(anyObject())

    activity.onResume()
    //verify(persistenceFactory, times(1)).refreshAfterDataChanged(anyObject())
  }

  @Test
  def shouldIgnoreClicksOnHeader() {
    val application = mock[CrudApplication]
    val crudType = MyCrudType
    val activity = new CrudListActivity(crudType, application)
    // should do nothing
    activity.onListItemClick(null, null, -1, -1)
  }
}