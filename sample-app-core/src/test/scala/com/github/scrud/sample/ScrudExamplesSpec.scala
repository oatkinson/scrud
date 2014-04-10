package com.github.scrud.sample

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.{MustMatchers, FunSpec}
import com.github.scrud.platform.{PlatformDriver, TestingPlatformDriver}
import com.github.scrud.action._
import com.github.scrud.context._
import com.github.scrud.copy.types.MapStorage
import com.github.scrud.copy.SourceType
import com.github.scrud.platform.representation.EditUI

/**
 * Examples of using scrud.
 * @author Eric Pabst (epabst@gmail.com)
 *         Date: 3/18/14
 *         Time: 5:30 PM
 */
@RunWith(classOf[JUnitRunner])
class ScrudExamplesSpec extends FunSpec with MustMatchers {
  val platformDriver: PlatformDriver = TestingPlatformDriver
  val entityNavigation = new SampleEntityNavigation(platformDriver)
  val entityTypeMap = entityNavigation.entityTypeMap
  val authorEntityType = entityTypeMap.authorEntityType

  describe("Normal Application Flow") {
    it("is easy to move through a normal user experience") {
      val sharedContext: SharedContext = new SimpleSharedContext(entityNavigation.entityTypeMap, platformDriver)
      val commandContext: CommandContext = sharedContext.asStubCommandContext

      val initialViewSpecifier = entityNavigation.initialViewSpecifier(commandContext)
      platformDriver.render(initialViewSpecifier, commandContext)

      initialViewSpecifier.entityNameOpt must be (Some(Author))
      initialViewSpecifier.entityIdOpt must be (None)
      val initialCommands = entityNavigation.usualAvailableCommandsForView(initialViewSpecifier)
      initialCommands.map(_.actionKeyAndEntityNameOrFail) must be (Seq(ActionKey.Create -> Author))
      initialCommands.map(_.entityIdOpt) must be (Seq(None))
      initialCommands.map(_.actionDataTypeOpt) must be (Seq(Some(EditUI)))

      val createAuthorCommand = initialCommands.head
//todo what about being able to have various commands available while editing???
//    val commandsAvailableFromCreate = entityNavigation.usualAvailableCommandsForView(createAuthorViewSpecifier)
//      commandsAvailableFromCreate.map(_.actionKey) must be (Seq(ActionKey.Create))
      val defaultAuthorData = authorEntityType.copyAndUpdate(SourceType.none, SourceType.none, MapStorage, commandContext)

      // Simulate a user providing some data
      val userModifiedActionData = authorEntityType.copyAndUpdate(MapStorage, new MapStorage(
        authorEntityType.nameField -> Some("George")), MapStorage, defaultAuthorData, commandContext)

      val newAuthorViewSpecifier = entityNavigation.invoke(createAuthorCommand, Some(userModifiedActionData), commandContext)
      newAuthorViewSpecifier.entityNameOpt must be (Some(Author))
      val Some(newAuthorId) = newAuthorViewSpecifier.entityIdOpt
      val commandsAvailableFromView = entityNavigation.usualAvailableCommandsForView(newAuthorViewSpecifier)
      commandsAvailableFromView.map(_.actionKeyAndEntityNameOrFail) must be (Seq(ActionKey.Update -> Author))

      sharedContext.withPersistence(_.find(Author, newAuthorId, authorEntityType.nameField, commandContext)) must be (Some("George"))
    }
  }
}
