package com.github.scrud.android.sample

import com.github.triangle.PortableField._
import com.github.scrud.android._
import persistence.CursorField._
import com.github.scrud.{EntityName, EntityType}
import persistence.PersistedType._
import java.util.Date
import com.github.scrud.Validation._
import com.github.scrud.platform.PlatformDriver
import com.github.scrud.types.{TitleQT, PositiveIntQT, EnumerationValueQT, DateWithoutTimeQT}

object Book extends EntityName("Book")

class BookEntityType(platformDriver: PlatformDriver) extends EntityType(Book, platformDriver) {
  val valueFields = List(
    ForeignKey[AuthorEntityType](Author),

    persisted[String]("name") + namedViewField("name", TitleQT) + requiredString,

    persisted[Int]("edition") + namedViewField("edition", PositiveIntQT),

    persistedEnum[Genre.Value]("genre", Genre) + namedViewField("genre", EnumerationValueQT[Genre.Value](Genre)) +
      default(Genre.Fantasy),

    ForeignKey[PublisherEntityType](Publisher, namedViewField("publisher", Publisher)),

    persistedDate("publishDate") + namedViewField[Date]("publishDate", DateWithoutTimeQT)
  )
}
