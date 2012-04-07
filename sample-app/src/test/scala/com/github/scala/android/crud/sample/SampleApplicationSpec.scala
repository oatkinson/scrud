package com.github.scala.android.crud.sample

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import com.github.scala.android.crud.persistence.CursorField._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import com.github.scala.android.crud._
import action.{ContextWithState, State}

/** A behavior specification for [[com.github.scala.android.crud.sample.AuthorEntityType]]
  * within [[com.github.scala.android.crud.sample.SampleApplication]].
  * @author Eric Pabst (epabst@gmail.com)
  */
@RunWith(classOf[JUnitRunner])
class SampleApplicationSpec extends Spec with MustMatchers with MockitoSugar {
  val application = new SampleApplication

  describe("Author") {
    it("must have the right children") {
      application.AuthorCrudType.childEntities(application) must
              be (List[CrudType](application.BookCrudType))
    }

    it("must calculate the book count") {
      val application = mock[CrudApplication]
      val crudContext = new CrudContext(mock[ContextWithState], application) {
        override val activityState = new State {}
      }
      val factory = GeneratedPersistenceFactory(new ListBufferCrudPersistence(Map.empty[String, Any], _, crudContext))
      val bookCrudType = new CrudType(BookEntityType, factory)
      val bookPersistence = bookCrudType.openEntityPersistence(crudContext).asInstanceOf[ListBufferCrudPersistence[Map[String,Any]]]
      bookPersistence.buffer += Map.empty[String,Any] += Map.empty[String,Any]

      stub(application.crudType(BookEntityType)).toReturn(bookCrudType)
      val authorData = AuthorEntityType.copyAndTransformWithItem(List(AuthorEntityType.toUri(100L), crudContext), Map.empty[String,Any])
      authorData must be (Map[String,Any](idFieldName -> 100L, "bookCount" -> 2))
    }
  }
}
