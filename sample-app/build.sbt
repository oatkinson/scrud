name := "Scrud Android Sample"

organization := "com.github.epabst.scrud"

version := General.scrudVersion

AndroidKeys.keyalias in AndroidKeys.Android := "change-me"

AndroidKeys.versionCode := 0

scalaVersion := "2.9.2"

AndroidKeys.platformName in AndroidKeys.Android := "android-10"

AndroidKeys.useProguard in AndroidKeys.Android := true

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.8" % "test"

libraryDependencies += "org.slf4j" % "slf4j-jdk14" % "1.6.1" % "test"

// Don't include this when running simple unit tests since slf4j-jdk14 should be used then.
fullClasspath in Test ~= { classpath => classpath.filter { entry => !entry.data.getName.contains("slf4j-android") } }

libraryDependencies += "org.mockito" % "mockito-core" % "1.8.5" % "test"

libraryDependencies += "junit" % "junit" % "4.8.2" % "test"

//todo eliminate easymock as a dependency
libraryDependencies += "org.easymock" % "easymock" % "2.5.2" % "test"
