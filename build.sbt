import Dependencies._

lazy val fluflu = (project in file("."))
  .settings(publish / skip := true)
  .settings(
    inThisBuild(
      Seq(
        organization := "com.github.tkrs",
        homepage := Some(url("https://github.com/tkrs/fluflu")),
        licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
        developers := List(
          Developer(
            "tkrs",
            "Takeru Sato",
            "type.in.type@gmail.com",
            url("https://github.com/tkrs")
          )
        ),
        scalaVersion := Ver.`scala2.13`,
        crossScalaVersions := Seq(Ver.`scala2.12`, Ver.`scala2.13`),
        libraryDependencies ++= (Pkg.forTest ++ Seq(Pkg.scalaLogging)),
        scalacOptions ++= compilerOptions ++ warnCompilerOptions ++ {
          CrossVersion.partialVersion(scalaVersion.value) match {
            case Some((2, n)) if n >= 13 => Nil
            case _                       => Seq("-Xfuture", "-Ypartial-unification", "-Yno-adapted-args")
          }
        },
        fork := true,
        scalafmtOnCompile := true,
        scalafixOnCompile := true,
        scalafixDependencies += OrganizeImports,
        semanticdbEnabled := true,
        semanticdbVersion := scalafixSemanticdb.revision
      )
    )
  )
  .settings(
    Compile / console / scalacOptions --= warnCompilerOptions,
    Compile / console / scalacOptions += "-Yrepl-class-based"
  )
  .aggregate(core, msgpack, `msgpack-mess`, it)
  .dependsOn(core, msgpack, `msgpack-mess`, it)

lazy val core = project
  .in(file("modules/core"))
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
  .settings(
    description := "fluflu msgpack",
    moduleName := "fluflu-msgpack",
    libraryDependencies ++= Seq(
      Pkg.msgpackJava
    )
  )

lazy val `msgpack-mess` = project
  .in(file("modules/msgpack-mess"))
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
  .settings(publish / skip := true)
  .settings(
    description := "fluflu it",
    moduleName := "fluflu-it",
    libraryDependencies += Pkg.logbackClassic
  )
  .dependsOn(core, `msgpack-mess` % "compile->compile;test->test")

lazy val examples = project
  .in(file("modules/examples"))
  .settings(publish / skip := true)
  .settings(
    description := "fluflu examples",
    moduleName := "fluflu-examples"
  )
  .settings(
    libraryDependencies += Pkg.logbackClassic
  )
  .settings(
    coverageEnabled := false
  )
  .dependsOn(core, `msgpack-mess`)

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
