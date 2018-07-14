import Deps._

ThisBuild / organization := "com.github.tkrs"
ThisBuild / scalaVersion := Ver.`scala2.12`
ThisBuild / crossScalaVersions := Seq(
  Ver.`scala2.11`,
  Ver.`scala2.12`,
)
ThisBuild / resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)
ThisBuild / libraryDependencies ++= Pkg.forTest ++ Seq(
  Pkg.scalaLogging,
  compilerPlugin(Pkg.kindProjector)
)
ThisBuild / scalacOptions ++= compilerOptions ++ {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, p)) if p >= 12 => warnCompilerOptions
    case _                       => Nil
  }
}
ThisBuild / Test / fork := true

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked",
  "-feature",
  "-language:_",
  "-Xfuture",
)

lazy val warnCompilerOptions = Seq(
  "-Xlint",
  "-Yno-adapted-args",
  "-Xfatal-warnings",
  "-Ywarn-extra-implicit",
  "-Ywarn-unused:_",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
)

lazy val fluflu = (project in file("."))
  .settings(noPublishSettings)
  .settings(
    Compile / console / scalacOptions --= compilerOptions ++ warnCompilerOptions,
    Compile / console / scalacOptions += "-Yrepl-class-based"
  )
  .aggregate(core, queue, monix, `monix-reactive`, msgpack, `msgpack-circe`, it, benchmark, examples)
  .dependsOn(core, queue, monix, `monix-reactive`, msgpack, `msgpack-circe`, it, benchmark, examples)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/tkrs/fluflu")),
  licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
  publishMavenStyle := true,
  Test / publishArtifact := false,
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
  publishTo := Some(Resolver.mavenLocal),
  publishLocal := ((): Unit),
  publishArtifact := false,
)

lazy val core = project.in(file("modules/core"))
  .settings(publishSettings)
  .settings(
    description := "fluflu core",
    moduleName := "fluflu-core",
  )
  .dependsOn(msgpack % "compile->compile;test->test")

lazy val queue = project.in(file("modules/queue"))
  .settings(publishSettings)
  .settings(
    description := "fluflu queue",
    moduleName := "fluflu-queue",
  )
  .dependsOn(core, msgpack % "compile->compile;test->test")

lazy val monix = project.in(file("modules/monix"))
  .settings(publishSettings)
  .settings(
    description := "fluflu monix",
    moduleName := "fluflu-monix",
  )
  .settings(
    libraryDependencies ++= Seq(
      Pkg.monixEval,
    )
  )
  .dependsOn(core, msgpack)

lazy val `monix-reactive` = project.in(file("modules/monix-reactive"))
  .settings(publishSettings)
  .settings(
    description := "fluflu monix-reactive",
    moduleName := "fluflu-monix-reactive",
  )
  .settings(
    libraryDependencies ++= Seq(
      Pkg.monixReactive,
    )
  )
  .dependsOn(core, msgpack)

lazy val msgpack = project.in(file("modules/msgpack"))
  .settings(publishSettings)
  .settings(
    description := "fluflu msgpack",
    moduleName := "fluflu-msgpack",
    libraryDependencies ++= Seq(
      Pkg.msgpackJava,
    )
  )

lazy val `msgpack-circe` = project.in(file("modules/msgpack-circe"))
  .settings(publishSettings)
  .settings(
    description := "fluflu msgpack-circe",
    moduleName := "fluflu-msgpack-circe",
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
  .settings(noPublishSettings)
  .settings(
    description := "fluflu it",
    moduleName := "fluflu-it",
  )
  .dependsOn(core, msgpack % "compile->compile;test->test")

lazy val examples = project.in(file("modules/examples"))
  .settings(noPublishSettings)
  .settings(
    description := "fluflu examples",
    moduleName := "fluflu-examples",
    fork := true,
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
  .settings(noPublishSettings)
  .settings(
    description := "fluflu benchmark",
    moduleName := "fluflu-benchmark",
  )
  .settings(
    coverageEnabled := false
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(`msgpack-circe` % "test->test")
