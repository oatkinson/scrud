package com.github.scrud.android.generate

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.github.triangle.{PortableField, ValueFormat}
import PortableField._
import com.github.scrud.android.view.ViewField._
import com.github.scrud.android.ForeignKey._
import com.github.scrud.android.testres.R
import com.github.scrud.android._
import org.scalatest.mock.MockitoSugar
import testres.R.id
import view.EntityView
import com.github.scrud.ParentField

/** A behavior specification for [[com.github.scrud.android.generate.EntityFieldInfo]].
  * @author Eric Pabst (epabst@gmail.com)
  */
@RunWith(classOf[JUnitRunner])
class EntityFieldInfoSpec extends FunSpec with MustMatchers with MockitoSugar {
  val application = MyCrudApplication(CrudType(MyEntityType, null))

  describe("viewFields") {
    it("must find all ViewFields") {
      val dummyFormat = ValueFormat[String](s => Some(s + "."), _.stripSuffix("."))
      val fieldList = mapField[String]("foo") + textView + formatted[String](dummyFormat, textView) + viewId(45, textView)
      val info = ViewIdFieldInfo("foo", fieldList)
      info.viewFields must be(List(textView, textView, textView))
    }
  }

  it("must handle a viewId name that does not exist") {
    val fieldInfo = EntityFieldInfo(viewId(classOf[R.id], "bogus", textView), List(classOf[R]), application).viewIdFieldInfos.head
    fieldInfo.id must be ("bogus")
  }

  it("must consider a ParentField displayable if it has a viewId field") {
    val fieldInfo = EntityFieldInfo(ParentField(MyEntity) + namedViewField("foo", longView), Seq(classOf[R]), application)
    fieldInfo.isDisplayable must be (true)
  }

  it("must not include a ParentField if it has no viewId field") {
    val fieldInfos = EntityFieldInfo(ParentField(MyEntity), Seq(classOf[R]), application).viewIdFieldInfos
    fieldInfos must be (Nil)
  }

  it("must not include adjustment fields") {
    val fieldInfos = EntityFieldInfo(adjustment[String](_ + "foo"), Seq(classOf[R]), application).viewIdFieldInfos
    fieldInfos must be (Nil)
  }

  it("must not include adjustmentInPlace fields") {
    val fieldInfos = EntityFieldInfo(adjustmentInPlace[StringBuffer] { s => s.append("foo"); Unit }, Seq(classOf[R]), application).viewIdFieldInfos
    fieldInfos must be (Nil)
  }

  it("must not include the default primary key field") {
    val fieldInfos = EntityFieldInfo(MyCrudType.entityType.IdField, Seq(classOf[R]), application).viewIdFieldInfos
    fieldInfos must be (Nil)
  }

  it("must not include a ForeignKey if it has no viewId field") {
    val fieldInfo = EntityFieldInfo(foreignKey(MyEntity), Seq(classOf[R]), application)
    fieldInfo.isUpdateable must be (false)
  }

  it("must detect multiple ViewFields in the same field") {
    val fieldInfos = EntityFieldInfo(viewId(R.id.foo, textView) + viewId(R.id.bar, textView), Seq(classOf[R.id]), application).viewIdFieldInfos
    fieldInfos.map(_.id) must be (List("foo", "bar"))
  }

  val entityFieldInfo = EntityFieldInfo(viewId(R.id.foo, foreignKey(MyEntity) + EntityView(MyEntity)), Seq(classOf[id]), application)

  describe("updateableViewIdFieldInfos") {
    it("must not include fields whose editXml is Empty") {
      val info = EntityFieldInfo(viewId(R.id.foo, textView.suppressEdit), Seq(classOf[id]), application)
      val fieldInfos = info.updateableViewIdFieldInfos
      fieldInfos must be ('empty)
    }

    it("must provide a single field for an EntityView field to allow choosing Entity instance") {
      val fieldInfos = entityFieldInfo.updateableViewIdFieldInfos
      fieldInfos.map(_.id) must be (List("foo"))
      fieldInfos.map(_.layout).head.editXml.head.label must be ("Spinner")
    }

    it("must not include fields whose childView field isn't a ViewField") {
      val info = EntityFieldInfo(viewId(R.id.foo, mapField[String]("foo")), Seq(classOf[id]), application)
      val fieldInfos = info.updateableViewIdFieldInfos
      fieldInfos must be ('empty)
    }
  }

  describe("displayableViewIdFieldInfos") {
    it("must not include fields whose displayXml is Empty") {
      val info = EntityFieldInfo(viewId(R.id.foo, textView.suppressDisplay), Seq(classOf[id]), application)
      val fieldInfos = info.displayableViewIdFieldInfos
      fieldInfos must be ('empty)
    }

    it("must provide each displayable field in the referenced EntityType for an EntityView field") {
      val fieldInfos = entityFieldInfo.displayableViewIdFieldInfos
      fieldInfos must be (EntityTypeViewInfo(MyEntityType, null).displayableViewIdFieldInfos)
    }

    it("must not include fields whose childView field isn't a ViewField") {
      val info = EntityFieldInfo(viewId(R.id.foo, mapField[String]("foo")), Seq(classOf[id]), application)
      val fieldInfos = info.displayableViewIdFieldInfos
      fieldInfos must be ('empty)
    }
  }
}
