name := "wildcat"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies ++= List(
  "com.github.cb372" %% "scalacache-cats-effect" % "0.28.0",
  "com.github.cb372" %% "scalacache-circe" % "0.28.0",
  "com.github.cb372" %% "scalacache-caffeine" % "0.28.0",
  "io.chrisdavenport" %% "log4cats-core" % "1.1.1",
  "io.chrisdavenport" %% "log4cats-slf4j" % "1.1.1",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)
