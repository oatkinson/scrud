package com.github.scrud.sample

import com.github.scrud
import scrud.platform.PlatformDriver
import scrud.types.TitleQT
import scrud.{EntityName, EntityType}
import com.github.scrud.platform.representation.{SelectUI, Persistence, EditUI}

object Publisher extends EntityName("Publisher")

class PublisherEntityType(platformDriver: PlatformDriver) extends EntityType(Publisher, platformDriver) {
  field("name", TitleQT, Seq(Persistence, EditUI, SelectUI)) //todo requiredString, Persistence(dataVersion = 2)
//    namedViewField("bookCount", NaturalIntQT) + bundleField[Int]("bookCount") + Getter[Int] {
//      case UriField(Some(uri)) && CrudContextField(Some(crudContext)) => {
//        println("calculating bookCount for " + uri + " and " + crudContext)
//        crudContext.withEntityPersistence(Book) { persistence =>
//          val books = persistence.findAll(uri)
//          Some(books.size)
//        }
//      }
//    }
}
