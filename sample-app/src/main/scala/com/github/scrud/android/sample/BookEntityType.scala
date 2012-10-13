package com.github.scrud.android.sample

import com.github.scrud.android._
import entity.EntityName
import persistence.CursorField._
import persistence.EntityType
import persistence.PersistedType._
import java.util.Date
import com.github.scrud.android.ParentField._
import view.ViewField._
import view.{EntityView, EnumerationView}
import com.github.scrud.android.validate.Validation._

object Book extends EntityName("Book")

object BookEntityType extends EntityType(Book) {
  def valueFields = List(
    foreignKey(AuthorEntityType),

    persisted[String]("name") + viewId(classOf[R], "name", textView) + requiredString,

    persisted[Int]("edition") + viewId(classOf[R], "edition", intView),

    persistedEnum[Genre.Value]("genre", Genre) + viewId(classOf[R], "genre", EnumerationView[Genre.Value](Genre)),

    foreignKey(PublisherEntityType) + viewId(classOf[R], "publisher", EntityView(PublisherEntityType)),

    persistedDate("publishDate") + viewId[Date](classOf[R], "publishDate", dateView)
  )
}
