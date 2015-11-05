lazy val root = (project in file("."))
  .settings(allSettings)
  .settings(noPublishSettings)
  .aggregate(core, msgpack)
  .dependsOn(core, msgpack)

lazy val allSettings = buildSettings ++ baseSettings

lazy val buildSettings = Seq(
  name := "fluflu",
  organization := "com.github.tkrs",
  scalaVersion := "2.11.7"
)

val circeVersion = "0.2.0"
val scalazVersion = "7.1.3"
val scalacheckVersion = "1.12.3"
val scalatestVersion = "2.2.5"
// val catsVersion = "0.2.0"

lazy val baseSettings = Seq(
  scalacOptions ++= compilerOptions,
  scalacOptions in (Compile, console) := compilerOptions,
  scalacOptions in (Compile, test) := compilerOptions,
  libraryDependencies ++= Seq(
    // "org.spire-math" %% "cats" % catsVersion,
    "org.scalaz" %% "scalaz-core" % scalazVersion,
    "org.scalaz" %% "scalaz-concurrent" % scalazVersion,
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-jawn" % circeVersion,
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
  ) ++ tests,
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    Resolver.bintrayRepo("scalaz", "releases")
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
      url("https://github.com/fluflu"),
      "scm:git:git@github.com:tkrs/fluflu.git"
    )
  ),
  pomExtra := (
    <developers>
      <developer>
        <id>tkrs</id>
        <name>Takeru Sato</name>
        <url>https://github.com/tkrs</url>
      </developer>
    </developers>
  )
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
    name := "core"
  )
  .settings(allSettings: _*)
  .dependsOn(msgpack)

lazy val msgpack = project.in(file("msgpack"))
  .settings(
    description := "fluflu msgpack",
    moduleName := "fluflu-msgpack",
    name := "msgpack"
  )
  .settings(allSettings: _*)

lazy val example = project.in(file("example"))
  .settings(
    description := "fluflu example",
    moduleName := "fluflu-example",
    name := "example"
  )
  .settings(allSettings: _*)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-simple" % "1.7.12"
    )
  )
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

lazy val tests = Seq(
  "org.scalatest" %% "scalatest" % scalatestVersion,
  "org.scalacheck" %% "scalacheck" % scalacheckVersion,
  "org.scalaz" %% "scalaz-scalacheck-binding" % scalazVersion
) map (_ % "test")

scalariformSettings
// wartremoverErrors ++= Warts.all
