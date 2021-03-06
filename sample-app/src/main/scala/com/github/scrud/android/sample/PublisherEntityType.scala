package com.github.scrud.android.sample

import com.github.scrud
import scrud.android.persistence.CursorField._
import scrud.platform.PlatformDriver
import scrud.types.{TitleQT, NaturalIntQT}
import scrud.{CrudContextField, EntityName, UriField, EntityType}
import com.github.triangle._
import scrud.Validation._

object Publisher extends EntityName("Publisher")

class PublisherEntityType(platformDriver: PlatformDriver) extends EntityType(Publisher, platformDriver) {
  val valueFields = List(
    persisted[String]("name") + namedViewField("name", TitleQT) + requiredString,

    namedViewField("bookCount", NaturalIntQT) + bundleField[Int]("bookCount") + Getter[Int] {
      case UriField(Some(uri)) && CrudContextField(Some(crudContext)) => {
        println("calculating bookCount for " + uri + " and " + crudContext)
        crudContext.withEntityPersistence(Book) { persistence =>
          val books = persistence.findAll(uri)
          Some(books.size)
        }
      }
    }
  )
}
