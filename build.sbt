import Dependencies._

ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "io.github.todokr"

val circeVersion = "0.12.3"
val doobieVersion = "0.9.0"

lazy val root = (project in file("."))
  .settings(
    name := "Refined with Newtype",
    libraryDependencies ++= Seq(
      "eu.timepit" %% "refined" % "0.9.13",
      "io.estatico" %% "newtype" % "0.4.3",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-refined" % circeVersion,
      "org.postgresql" % "postgresql" % "42.2.16",
      "org.tpolecat"  %% "doobie-core"      % doobieVersion,
      "org.tpolecat"  %% "doobie-postgres"  % doobieVersion,
      "org.tpolecat"  %% "doobie-refined"   % doobieVersion,
      compilerPlugin(
          "org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full
        ),
    ),
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-Ywarn-dead-code",
      "-Ymacro-annotations",
    )
)
