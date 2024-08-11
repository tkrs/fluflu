import Dependencies._

lazy val fluflu = project
  .in(file("."))
  .settings(
    inThisBuild(
      Seq(
        organization := "com.github.tkrs",
        homepage     := Some(url("https://github.com/tkrs/fluflu")),
        licenses     := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
        developers := List(
          Developer(
            "tkrs",
            "Takeru Sato",
            "type.in.type@gmail.com",
            url("https://github.com/tkrs")
          )
        ),
        scalaVersion       := Ver.scala3,
        crossScalaVersions := Seq(Ver.`scala2.13`, Ver.scala3),
        scalacOptions ++= {
          CrossVersion.partialVersion(scalaVersion.value) match {
            case Some((3, _)) => Nil
            case _            => compilerOptions ++ warnCompilerOptions
          }
        },
        fork              := true,
        scalafmtOnCompile := true,
        scalafixOnCompile := true,
        semanticdbEnabled := true,
        semanticdbVersion := scalafixSemanticdb.revision
      )
    )
  )
  .settings(
    Compile / console / scalacOptions --= warnCompilerOptions,
    Compile / console / scalacOptions += "-Yrepl-class-based"
  )
  .settings(publish / skip := true)
  .aggregate(core, msgpack, `msgpack-mess`)
  .dependsOn(core, msgpack, `msgpack-mess`)

lazy val core = project
  .in(file("modules/core"))
  .settings(
    description := "fluflu core",
    moduleName  := "fluflu-core"
  )
  .settings(
    libraryDependencies ++= Pkg.forTest.map(_ % Test) ++ Seq(Pkg.msgpackJava, Pkg.scalaLogging),
    Test / javaOptions += "-Dnet.bytebuddy.experimental=true"
  )
  .dependsOn(msgpack % "compile->compile;test->test")

lazy val msgpack = project
  .in(file("modules/msgpack"))
  .settings(
    description := "fluflu msgpack",
    moduleName  := "fluflu-msgpack",
    libraryDependencies ++= Pkg.forTest.map(_ % Test) ++ Seq(Pkg.msgpackJava)
  )

lazy val `msgpack-mess` = project
  .in(file("modules/msgpack-mess"))
  .settings(
    description := "fluflu msgpack-mess",
    moduleName  := "fluflu-msgpack-mess"
  )
  .settings(libraryDependencies ++= Pkg.forTest.map(_ % Test) ++ Seq(Pkg.mess))
  .dependsOn(msgpack % "compile->compile;test->test")

lazy val examples = project
  .in(file("modules/examples"))
  .settings(publish / skip := true)
  .settings(
    description := "fluflu examples",
    moduleName  := "fluflu-examples"
  )
  .settings(libraryDependencies ++= Pkg.forTest.map(_ % Test) ++ Seq(Pkg.logbackClassic))
  .settings(coverageEnabled := false)
  .dependsOn(core, `msgpack-mess`)

lazy val it = project
  .in(file("modules/it"))
  .settings(publish / skip := true)
  .settings(libraryDependencies ++= Pkg.forTest)
  .dependsOn(core, msgpack % "test->test", `msgpack-mess`)

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
