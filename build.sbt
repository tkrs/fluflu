lazy val root = (project in file("."))
  .settings(allSettings)
  .settings(noPublishSettings)
  .aggregate(core, queue, sf, msgpack, tests)
  .dependsOn(core, queue, sf, msgpack)

lazy val allSettings = buildSettings ++ baseSettings ++ publishSettings ++ scalariformSettings

lazy val buildSettings = Seq(
  name := "fluflu",
  organization := "com.github.tkrs",
  scalaVersion := "2.12.0",
  crossScalaVersions := Seq("2.11.8", "2.12.0")
)

val catsVersion = "0.8.1"
val circeVersion = "0.6.1"
val scalacheckVersion = "1.13.4"
val scalatestVersion = "3.0.1"

lazy val baseSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0"
  ),
  scalacOptions ++= compilerOptions,
  scalacOptions in (Compile, test) := compilerOptions,
  scalacOptions in (Compile, console) ~= (_ filterNot (_ == "-Ywarn-unused-import"))
)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
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
    </developers>,
  pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toCharArray)
) ++ credentialSettings

lazy val credentialSettings = Seq(
  credentials ++= (for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq
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

lazy val queue = project.in(file("queue"))
  .settings(
    description := "fluflu queue",
    moduleName := "fluflu-queue",
    name := "queue",
    scalaVersion := "2.11.8",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats" % catsVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
    )
  )
  .settings(allSettings: _*)
  .dependsOn(core, msgpack)

lazy val sf = project.in(file("sf"))
  .settings(
    description := "fluflu scala future",
    moduleName := "fluflu-sf",
    name := "sf"
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

lazy val benchmark = (project in file("benchmark"))
  .settings(
    description := "fluflu benchmark",
    moduleName := "fluflu-benchmark",
    scalaVersion := "2.12.0"
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(msgpack)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-unused-import",
  "-Ywarn-numeric-widen",
  "-Xfuture",
  "-Xlint"
)
