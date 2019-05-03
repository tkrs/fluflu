import Deps._

ThisBuild / organization := "com.github.tkrs"
ThisBuild / scalaVersion := Ver.`scala2.13`
ThisBuild / crossScalaVersions := Seq(
  Ver.`scala2.12`,
  Ver.`scala2.13`
)
ThisBuild / resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)
ThisBuild / libraryDependencies ++= Pkg.forTest ++ Seq(Pkg.scalaLogging, compilerPlugin(Pkg.kindProjector))
ThisBuild / scalacOptions ++= compilerOptions ++ {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, n)) if n >= 13 => warnCompilerOptions
    case _                       => warnCompilerOptions ++ Seq("-Xfuture", "-Ypartial-unification", "-Yno-adapted-args")
  }
}
ThisBuild / Test / fork := true
ThisBuild / scalafmtOnCompile := true

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-unchecked",
  "-feature",
  "-language:_"
)

lazy val warnCompilerOptions = Seq(
  "-Xlint",
  // "-Xfatal-warnings",
  "-Ywarn-extra-implicit",
  "-Ywarn-unused:_",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen"
)

lazy val fluflu = (project in file("."))
  .settings(publishSettings)
  .settings(noPublishSettings)
  .settings(
    Compile / console / scalacOptions --= compilerOptions ++ warnCompilerOptions,
    Compile / console / scalacOptions += "-Yrepl-class-based"
  )
  .aggregate(core, msgpack, `msgpack-mess`, it)
  .dependsOn(core, msgpack, `msgpack-mess`, it)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/tkrs/fluflu")),
  licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ =>
    false
  },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots".at(nexus + "content/repositories/snapshots"))
    else
      Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
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
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val core = project
  .in(file("modules/core"))
  .settings(publishSettings)
  .settings(
    description := "fluflu core",
    moduleName := "fluflu-core"
  )
  .settings(
    libraryDependencies += Pkg.msgpackJava,
    Test / javaOptions += "-Dnet.bytebuddy.experimental=true"
  )
  .dependsOn(msgpack % "compile->compile;test->test")

lazy val msgpack = project
  .in(file("modules/msgpack"))
  .settings(publishSettings)
  .settings(
    description := "fluflu msgpack",
    moduleName := "fluflu-msgpack",
    libraryDependencies ++= Seq(
      Pkg.msgpackJava
    )
  )

lazy val `msgpack-mess` = project
  .in(file("modules/msgpack-mess"))
  .settings(publishSettings)
  .settings(
    description := "fluflu msgpack-mess",
    moduleName := "fluflu-msgpack-mess"
  )
  .settings(
    libraryDependencies += Pkg.mess
  )
  .dependsOn(msgpack % "compile->compile;test->test")

lazy val it = project
  .in(file("modules/it"))
  .settings(publishSettings)
  .settings(noPublishSettings)
  .settings(
    description := "fluflu it",
    moduleName := "fluflu-it",
    libraryDependencies += Pkg.logbackClassic
  )
  .dependsOn(core, `msgpack-mess` % "compile->compile;test->test")

lazy val examples = project
  .in(file("modules/examples"))
  .settings(publishSettings)
  .settings(noPublishSettings)
  .settings(
    description := "fluflu examples",
    moduleName := "fluflu-examples",
    fork := true
  )
  .settings(
    libraryDependencies += Pkg.logbackClassic
  )
  .settings(
    coverageEnabled := false
  )
  .dependsOn(core, `msgpack-mess`)
