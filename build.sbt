lazy val root = (project in file("."))
  .settings(allSettings)
  .settings(noPublishSettings)
  .aggregate(core, queue, sf, msgpack, tests)
  .dependsOn(core, queue, sf, msgpack)

lazy val allSettings = buildSettings ++ baseSettings ++ publishSettings ++ scalariformSettings

lazy val buildSettings = Seq(
  name := "fluflu",
  organization := "com.github.tkrs",
  scalaVersion := "2.11.8"
)

val catsVersion = "0.7.2"
val catbirdVersion = "0.7.0"
val circeVersion = "0.5.4"
val scalacheckVersion = "1.13.4"
val scalatestVersion = "3.0.0"

lazy val baseSettings = Seq(
  scalacOptions ++= compilerOptions,
  scalacOptions in (Compile, console) := compilerOptions,
  scalacOptions in (Compile, test) := compilerOptions,
  libraryDependencies ++= Seq(
    "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0"
  )
)

lazy val publishSettings = Seq(
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/tkrs/fluflu")),
  licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/tkrs/fluflu"),
      "scm:git:git@github.com:tkrs/fluflu.git"
    )
  ),
  pomExtra :=
    <developers>
      <developer>
        <id>tkrs</id>
        <name>Takeru Sato</name>
        <url>https://github.com/tkrs</url>
      </developer>
    </developers>
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val core = project.in(file("core"))
  .settings(
    description := "fluflu core",
    moduleName := "fluflu-core",
    name := "core",
    scalaVersion := "2.11.8"
  )
  .settings(allSettings: _*)
  .dependsOn(msgpack)

lazy val queue = project.in(file("queue"))
  .settings(
    description := "fluflu queue",
    moduleName := "fluflu-queue",
    name := "queue",
    scalaVersion := "2.11.8",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats" % catsVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
    )
  )
  .settings(allSettings: _*)
  .dependsOn(core, msgpack)

lazy val sf = project.in(file("sf"))
  .settings(
    description := "fluflu scala future",
    moduleName := "fluflu-sf",
    name := "sf",
    scalaVersion := "2.11.8"
  )
  .settings(allSettings: _*)
  .dependsOn(core, msgpack)

lazy val msgpack = project.in(file("msgpack"))
  .settings(
    description := "fluflu msgpack",
    moduleName := "fluflu-msgpack",
    name := "msgpack",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion
    )
  )
  .settings(allSettings: _*)

lazy val examples = project.in(file("examples"))
  .settings(
    description := "fluflu examples",
    moduleName := "fluflu-examples",
    name := "examples"
  )
  .settings(allSettings: _*)
  .settings(noPublishSettings)
  .settings(
    fork := true,
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3"
  )
  .dependsOn(queue)

lazy val tests = project.in(file("tests"))
  .settings(
    description := "fluflu tests",
    moduleName := "fluflu-tests",
    name := "tests"
  )
  .settings(allSettings: _*)
  .settings(noPublishSettings)
  .settings(fork in test := true)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % scalatestVersion,
      "org.scalacheck" %% "scalacheck" % scalacheckVersion
    )
  )
  .settings(fork := true)
  .dependsOn(core, msgpack)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture",
  "-Yinline-warnings",
  "-Xlint",
  "-Ybackend:GenBCode",
  "-Ydelambdafy:method",
  "-target:jvm-1.8",
  "-optimize",
  "-Yopt:l:classpath"
)
