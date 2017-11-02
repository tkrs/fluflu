import Deps._

lazy val root = (project in file("."))
  .settings(allSettings)
  .settings(noPublishSettings)
  .aggregate(core, queue, monix, msgpack, `msgpack-circe`, tests)
  .dependsOn(core, queue, monix, msgpack, `msgpack-circe`)

lazy val allSettings = buildSettings ++ baseSettings ++ publishSettings

lazy val buildSettings = Seq(
  name := "fluflu",
  organization := "com.github.tkrs",
  scalaVersion := Ver.`scala2.12`,
  crossScalaVersions := Seq(
    Ver.`scala2.11`,
    Ver.`scala2.12`,
  ),
)

lazy val baseSettings = Seq(
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  libraryDependencies ++= Seq(
    Pkg.scalaLogging,
    Pkg.scalaJava8Compat,
  ),
  scalacOptions ++= compilerOptions ++ Seq("-Ywarn-unused-import"),
  scalacOptions in (Compile, console) ~= (_ filterNot (_ == "-Ywarn-unused-import")),
  scalacOptions in (Compile, console) += "-Yrepl-class-based",
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
      "scm:git:git@github.com:tkrs/fluflu.git",
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
  pgpSecretRing := sys.env.get("PGP_SECRET_RING").fold(pgpSecretRing.value)(file),
) ++ credentialSettings

lazy val credentialSettings = Seq(
  credentials ++= (for {
    username <- sys.env.get("SONATYPE_USERNAME")
    password <- sys.env.get("SONATYPE_PASSWORD")
  } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq
)

lazy val noPublishSettings = Seq(
  publish := ((): Unit),
  publishLocal := ((): Unit),
  publishArtifact := false,
)

lazy val core = project.in(file("core"))
  .settings(allSettings)
  .settings(
    description := "fluflu core",
    moduleName := "fluflu-core",
    name := "core",
  )
  .dependsOn(msgpack)

lazy val queue = project.in(file("queue"))
  .settings(allSettings)
  .settings(
    description := "fluflu queue",
    moduleName := "fluflu-queue",
    name := "queue",
  )
  .dependsOn(core, msgpack)

lazy val monix = project.in(file("monix"))
  .settings(allSettings)
  .settings(
    description := "fluflu monix",
    moduleName := "fluflu-monix",
    name := "monix",
    libraryDependencies ++= Seq(
      Pkg.monixEval,
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
      Pkg.msgpackJava,
    )
  )

lazy val `msgpack-circe` = project.in(file("msgpack-circe"))
  .settings(allSettings)
  .settings(
    description := "fluflu msgpack-circe",
    moduleName := "fluflu-msgpack-circe",
    name := "msgpack-circe",
    libraryDependencies ++= Seq(
      Pkg.circeCore,
      Pkg.circeGeneric,
      Pkg.circeParser,
    )
  )
  .dependsOn(msgpack)

lazy val examples = project.in(file("examples"))
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    description := "fluflu examples",
    moduleName := "fluflu-examples",
    name := "examples",
    crossScalaVersions := Seq(Ver.`scala2.12`),
    fork := true,
    libraryDependencies ++= Seq(
      Pkg.monixReactive,
      Pkg.logbackClassic,
    )
  )
  .dependsOn(queue, monix, `msgpack-circe`)

lazy val tests = project.in(file("tests"))
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    description := "fluflu tests",
    moduleName := "fluflu-tests",
    name := "tests",
    libraryDependencies ++= Pkg.forTest,
  )
  .settings(fork in test := true)
  .settings(fork := true)
  .dependsOn(core, monix, queue, `msgpack-circe`)

lazy val benchmark = (project in file("benchmark"))
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    description := "fluflu benchmark",
    moduleName := "fluflu-benchmark",
    name := "benchmark",
    crossScalaVersions := Seq(Ver.`scala2.12`),
    libraryDependencies ++= Pkg.forTest,
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(`msgpack-circe`)

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
  "-Xlint",
)

addCommandAlias("cleanAll", ";clean;benchmark/clean;examples/clean")
addCommandAlias("compileAll", ";compile;benchmark/compile;examples/compile")
