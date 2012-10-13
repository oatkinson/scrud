package com.github.scrud.android.sample

import com.github.scrud
import scrud.android._
import entity.EntityName
import scrud.android.persistence.CursorField._
import scrud.android.persistence.EntityType
import scrud.android.view.ViewField._
import com.github.triangle._
import scrud.android.validate.Validation._

object Publisher extends EntityName("Publisher")

object PublisherEntityType extends EntityType(Publisher) {
  def valueFields = List(
    persisted[String]("name") + viewId(classOf[R], "publisher_name", textView) + requiredString,

    viewId(classOf[R], "bookCount", intView) + bundleField[Int]("bookCount") + Getter[Int] {
      case UriField(Some(uri)) && CrudContextField(Some(crudContext)) => {
        println("calculating bookCount for " + uri + " and " + crudContext)
        crudContext.withEntityPersistence(BookEntityType) { persistence =>
          val books = persistence.findAll(uri)
          Some(books.size)
        }
      }
    }
  )
}
