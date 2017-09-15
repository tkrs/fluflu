lazy val root = (project in file("."))
  .settings(allSettings)
  .settings(noPublishSettings)
  .aggregate(core, queue, monix, msgpack, tests)
  .dependsOn(core, queue, monix, msgpack)

lazy val allSettings = buildSettings ++ baseSettings ++ publishSettings

lazy val buildSettings = Seq(
  name := "fluflu",
  organization := "com.github.tkrs",
  scalaVersion := "2.12.3",
  crossScalaVersions := Seq("2.11.11", "2.12.3")
)

val catsVersion = "1.0.0-MF"
val circeVersion = "0.9.0-M1"
val scalacheckVersion = "1.13.5"
val scalatestVersion = "3.0.3"
val monixVersion = "2.3.0"

lazy val baseSettings = Seq(
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  libraryDependencies ++= Seq(
    "io.monix" %% "monix-eval" % monixVersion,
    "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0"
  ),
  scalacOptions ++= compilerOptions ++ Seq("-Ywarn-unused-import"),
  scalacOptions in (Compile, console) ~= (_ filterNot (_ == "-Ywarn-unused-import")),
  scalacOptions in (Compile, console) += "-Yrepl-class-based"
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
  pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toCharArray),
  pgpSecretRing := sys.env.get("PGP_SECRET_RING").fold(pgpSecretRing.value)(file)
) ++ credentialSettings

lazy val credentialSettings = Seq(
  credentials ++= (for {
    username <- sys.env.get("SONATYPE_USERNAME")
    password <- sys.env.get("SONATYPE_PASSWORD")
  } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val core = project.in(file("core"))
  .settings(allSettings)
  .settings(
    description := "fluflu core",
    moduleName := "fluflu-core",
    name := "core",
    libraryDependencies ++= Seq(
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
    )
  )
  .dependsOn(msgpack)

lazy val queue = project.in(file("queue"))
  .settings(allSettings)
  .settings(
    description := "fluflu queue",
    moduleName := "fluflu-queue",
    name := "queue",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % catsVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
    )
  )
  .dependsOn(core, msgpack)

lazy val monix = project.in(file("monix"))
  .settings(allSettings)
  .settings(
    description := "fluflu monix",
    moduleName := "fluflu-monix",
    name := "monix",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % catsVersion,
      "io.monix" %% "monix-eval" % monixVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
    )
  )
  .dependsOn(core, msgpack)

lazy val msgpack = project.in(file("msgpack"))
  .settings(allSettings)
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

lazy val examples = project.in(file("examples"))
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    description := "fluflu examples",
    moduleName := "fluflu-examples",
    name := "examples",
    scalaVersion := "2.12.3",
    crossScalaVersions := Seq("2.12.3"),
    fork := true,
    libraryDependencies ++= Seq(
      "io.monix" %% "monix-reactive" % monixVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    )
  )
  .dependsOn(queue, monix)

lazy val tests = project.in(file("tests"))
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    description := "fluflu tests",
    moduleName := "fluflu-tests",
    name := "tests",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % scalatestVersion,
      "org.scalacheck" %% "scalacheck" % scalacheckVersion
    )
  )
  .settings(fork in test := true)
  .settings(fork := true)
  .dependsOn(core, monix, queue, msgpack)

lazy val benchmark = (project in file("benchmark"))
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    description := "fluflu benchmark",
    moduleName := "fluflu-benchmark",
    name := "benchmark",
    scalaVersion := "2.12.3",
    crossScalaVersions := Seq("2.12.3"),
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % scalatestVersion,
      "org.scalacheck" %% "scalacheck" % scalacheckVersion
    )
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
  "-Ywarn-numeric-widen",
  "-Xfuture",
  "-Xlint"
)
