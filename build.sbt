import java.nio.file.Files
import Dependencies._

lazy val fluflu = project
  .in(file("."))
  .settings(
    inThisBuild(
      Seq(
        version      := Files.readString(file("version.txt").toPath).trim,
        organization := "com.github.tkrs",
        homepage     := Some(url("https://github.com/tkrs/fluflu")),
        licenses     := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
        developers   := List(
          Developer(
            "tkrs",
            "Takeru Sato",
            "type.in.type@gmail.com",
            url("https://github.com/tkrs")
          )
        ),
        scalaVersion       := Ver.scala3,
        crossScalaVersions := Seq(Ver.scala2, Ver.scala3),
        fork               := true,
        scalafmtOnCompile  := true,
        scalafixOnCompile  := true,
        semanticdbEnabled  := true,
        semanticdbVersion  := scalafixSemanticdb.revision
      )
    )
  )
  .settings(
    Compile / console / scalacOptions --= warnCompilerOptions,
    Compile / console / scalacOptions += "-Yrepl-class-based"
  )
  .settings(publish / skip := true)
  .aggregate(core)
  .dependsOn(core)

lazy val core = project
  .in(file("modules/core"))
  .settings(
    description := "fluflu core",
    moduleName  := "fluflu-core"
  )
  .settings(sharedSettings)
  .settings(
    libraryDependencies ++= Seq(
      Pkg.mess,
      Pkg.msgpackJava,
      Pkg.scalaLogging,
      Pkg.scalatest,
      Pkg.scalacheck,
      Pkg.mockito
    ),
    Test / javaOptions += "-Dnet.bytebuddy.experimental=true"
  )

lazy val examples = project
  .in(file("modules/examples"))
  .settings(publish / skip := true)
  .settings(
    description := "fluflu examples",
    moduleName  := "fluflu-examples"
  )
  .settings(sharedSettings)
  .settings(libraryDependencies ++= Seq(Pkg.logbackClassic))
  .settings(coverageEnabled := false)
  .dependsOn(core)

lazy val it = project
  .in(file("modules/it"))
  .settings(publish / skip := true)
  .settings(sharedSettings)
  .settings(
    libraryDependencies ++= Seq(
      Pkg.scalatest,
      Pkg.scalacheck,
      Pkg.mockito
    )
  )
  .dependsOn(core % "test->test")

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding",
  "utf-8",
  "-explaintypes",
  "-feature",
  "-language:higherKinds",
  "-unchecked"
)

lazy val warnCompilerOptions = Seq(
  // "-Xlint",
  "-Xcheckinit",
  // "-Xfatal-warnings",
  "-Wunused:_",
  "-Ywarn-extra-implicit",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen"
)

lazy val sharedSettings = Seq(
  scalacOptions ++= compilerOptions ++ {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) => Seq("-Wunused:imports")
      case _            => compilerOptions ++ warnCompilerOptions ++ Seq("-Xsource:3")
    }
  }
)
