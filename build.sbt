import Deps._

lazy val root = (project in file("."))
  .settings(allSettings)
  .settings(noPublishSettings)
  .aggregate(core, queue, monix, `monix-reactive`, msgpack, `msgpack-circe`, it, benchmark, examples)
  .dependsOn(core, queue, monix, `monix-reactive`, msgpack, `msgpack-circe`, it, benchmark, examples)

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
  libraryDependencies ++= Seq(Pkg.scalaLogging) ++ Pkg.forTest,
  scalacOptions ++= compilerOptions ++ Seq("-Ywarn-unused-import"),
  scalacOptions in (Compile, console) ~= (_ filterNot (_ == "-Ywarn-unused-import")),
  scalacOptions in (Compile, console) += "-Yrepl-class-based",
  fork in Test := true,
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
  .dependsOn(msgpack % "compile->compile;test->test")

lazy val queue = project.in(file("modules/queue"))
  .settings(allSettings)
  .settings(
    description := "fluflu queue",
    moduleName := "fluflu-queue",
    name := "queue",
  )
  .dependsOn(core, msgpack % "compile->compile;test->test")

lazy val monix = project.in(file("modules/monix"))
  .settings(allSettings)
  .settings(
    description := "fluflu monix",
    moduleName := "fluflu-monix",
    name := "monix",
  )
  .settings(
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
  )
  .settings(
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
  )
  .settings(
    libraryDependencies ++= Seq(
      Pkg.circeCore,
      Pkg.circeGeneric,
      Pkg.circeParser,
    )
  )
  .dependsOn(msgpack % "compile->compile;test->test")

lazy val it = project.in(file("modules/it"))
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    description := "fluflu it",
    moduleName := "fluflu-it",
    name := "it",
  )
  .dependsOn(core, msgpack % "compile->compile;test->test")

lazy val examples = project.in(file("modules/examples"))
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    description := "fluflu examples",
    moduleName := "fluflu-examples",
    name := "examples",
    fork in run := true,
  )
  .settings(
    libraryDependencies ++= Seq(
      Pkg.monixReactive,
      Pkg.logbackClassic,
    )
  )
  .settings(
    coverageEnabled := false
  )
  .dependsOn(queue, monix, `msgpack-circe`)

lazy val benchmark = (project in file("modules/benchmark"))
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    description := "fluflu benchmark",
    moduleName := "fluflu-benchmark",
    name := "benchmark",
  )
  .settings(
    coverageEnabled := false
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(`msgpack-circe` % "test->test")

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
