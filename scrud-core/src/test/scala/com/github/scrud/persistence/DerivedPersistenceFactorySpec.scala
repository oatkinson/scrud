package com.github.scrud.persistence

//import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.mockito.Mockito._
import com.github.scrud.util.CrudMockitoSugar
import com.github.scrud._
import com.github.scrud.context._
import com.github.scrud.EntityName

/** A specification for [[com.github.scrud.persistence.DerivedPersistenceFactory]].
  * @author Eric Pabst (epabst@gmail.com)
  */
//@RunWith(classOf[JUnitRunner])
class DerivedPersistenceFactorySpec extends FunSpec with MustMatchers with CrudMockitoSugar {
  it("must make the CrudPersistence for the delegate EntityNames available") {
    val entity1 = EntityName("entity1")
    val entity2 = EntityName("entity2")
    val persistence1 = mock[ThinPersistence]
    val persistence2 = mock[ThinPersistence]
    val factory = new DerivedPersistenceFactory[String](entity1, entity2) {
      def findAll(entityType: EntityType, uri: UriPath, persistenceConnection: PersistenceConnection) = {
        persistenceConnection.persistenceFor(entity1) must not be null
        persistenceConnection.persistenceFor(entity2) must not be null
        List("findAll", "was", "called")
      }
    }
    val persistenceFactory1 = new PersistenceFactoryForTesting(new EntityTypeForTesting(entity1), persistence1)
    val persistenceFactory2 = new PersistenceFactoryForTesting(new EntityTypeForTesting(entity2), persistence2)
    val entityTypeMap = new EntityTypeMap(persistenceFactory1.toTuple, persistenceFactory2.toTuple)
    val sharedContext = new SharedContextForTesting(entityTypeMap)
    val persistenceConnection = new PersistenceConnection(entityTypeMap, sharedContext)
    val persistence = factory.createEntityPersistence(mock[EntityType], persistenceConnection)
    persistence.findAll(UriPath()) must be (List("findAll", "was", "called"))
  }

  it("must close each used delegate CrudPersistence when close is called") {
    val entity1 = EntityName("entity1")
    val entity2 = EntityName("entity2")
    val persistence1 = mock[ThinPersistence]
    val persistence2 = mock[ThinPersistence]
    val entityTypeMap = EntityTypeMap(
      new PersistenceFactoryForTesting(new EntityTypeForTesting(entity1), persistence1).toTuple,
      new PersistenceFactoryForTesting(new EntityTypeForTesting(entity2), persistence2).toTuple)
    val sharedContext = new SharedContextForTesting(entityTypeMap)
    val persistenceConnection = new PersistenceConnection(entityTypeMap, sharedContext)
    val factory = new DerivedPersistenceFactory[String](entity1, entity2) {
      def findAll(entityType: EntityType, uri: UriPath, persistenceConnection: PersistenceConnection) = {
        persistenceConnection.persistenceFor(entity1) must not be null
        persistenceConnection.persistenceFor(entity2) must not be null
        Nil
      }
    }
    val derivedCrudPersistence = factory.createEntityPersistence(mock[EntityType], persistenceConnection)
    derivedCrudPersistence.findAll(UriPath.EMPTY, null)
    persistenceConnection.close()
    verify(persistence1).close()
    verify(persistence2).close()
  }
}