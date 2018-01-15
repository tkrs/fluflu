import Deps._

lazy val root = (project in file("."))
  .settings(allSettings)
  .settings(noPublishSettings)
  .aggregate(core, queue, monix, `monix-reactive`, msgpack, `msgpack-circe`, tests)
  .dependsOn(core, queue, monix, `monix-reactive`, msgpack, `msgpack-circe`)

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

lazy val core = project.in(file("modules/core"))
  .settings(allSettings)
  .settings(
    description := "fluflu core",
    moduleName := "fluflu-core",
    name := "core",
  )
  .dependsOn(msgpack)

lazy val queue = project.in(file("modules/queue"))
  .settings(allSettings)
  .settings(
    description := "fluflu queue",
    moduleName := "fluflu-queue",
    name := "queue",
  )
  .dependsOn(core, msgpack)

lazy val monix = project.in(file("modules/monix"))
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

lazy val `monix-reactive` = project.in(file("modules/monix-reactive"))
  .settings(allSettings)
  .settings(
    description := "fluflu monix-reactive",
    moduleName := "fluflu-monix-reactive",
    name := "monix-reactive",
    libraryDependencies ++= Seq(
      Pkg.monixReactive,
    )
  )
  .dependsOn(core, msgpack)

lazy val msgpack = project.in(file("modules/msgpack"))
  .settings(allSettings)
  .settings(
    description := "fluflu msgpack",
    moduleName := "fluflu-msgpack",
    name := "msgpack",
    libraryDependencies ++= Seq(
      Pkg.msgpackJava,
    )
  )

lazy val `msgpack-circe` = project.in(file("modules/msgpack-circe"))
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

lazy val examples = project.in(file("modules/examples"))
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

lazy val tests = project.in(file("modules/tests"))
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    description := "fluflu tests",
    moduleName := "fluflu-tests",
    name := "tests",
    libraryDependencies ++= Pkg.forTest,
  )
  .settings(fork in Test := true)
  .dependsOn(core, monix, `monix-reactive`, queue, `msgpack-circe`)

lazy val benchmark = (project in file("modules/benchmark"))
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
  "-Xlint",
  "-unchecked",
  "-Xfatal-warnings",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture",
  "-Xlint",
)

addCommandAlias("cleanAll", ";clean;benchmark/clean;examples/clean")
addCommandAlias("compileAll", ";compile;benchmark/compile;examples/compile")
