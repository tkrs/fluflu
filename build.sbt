lazy val root = (project in file("."))
  .settings(allSettings)

//lazy val itSettings = (project in file("."))
//  .configs(IntegrationTest)
//  .settings(allSettings)
//  .settings(Defaults.itSettings: _*)
//  .settings(
//    libraryDependencies += specs2core % "it"
//  )

lazy val allSettings = buildSettings ++ baseSettings

lazy val buildSettings = Seq(
  name := "fluflu",
  organization := "com.github.tkrs",
  version := "0.1.1-SNAPSHOT",
  scalaVersion := "2.11.7"
)

lazy val baseSettings = Seq(
  scalacOptions ++= compilerOptions,
  scalacOptions in (Compile, console) := compilerOptions,
  scalacOptions in (Compile, test) := compilerOptions,
  libraryDependencies ++= deps,
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    Resolver.bintrayRepo("scalaz", "releases")
  )
)

lazy val scalazVersion = "7.1.3"
lazy val scalacheckVersion = "1.12.3"
lazy val scalatestVersion = "2.2.5"

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture",
  "-Yinline-warnings",
  "-Xlint",
  "-Ybackend:GenBCode",
  "-Ydelambdafy:method",
  "-target:jvm-1.8",
  "-optimize",
  "-Yopt:l:classpath"
)

lazy val deps = (scalaz ++ others ++ tests) map (_.withSources())

// lazy val specs2core = "org.specs2" %% "specs2-core" % "2.4.14"

lazy val scalaz = Seq(
  "org.scalaz" %% "scalaz-core" % scalazVersion,
  "org.scalaz" %% "scalaz-concurrent" % scalazVersion,
  "org.scalaz" %% "scalaz-scalacheck-binding" % scalazVersion % "test"
)

lazy val others = Seq(
  "io.argonaut" %% "argonaut" % "6.0.4",
  "com.github.xuwei-k" % "msgpack4z-api" % "0.1.0",
  "com.github.xuwei-k" %% "msgpack4z-core" % "0.1.4",
  "com.github.xuwei-k" %% "msgpack4z-native" % "0.1.1",
  "com.github.xuwei-k" %% "msgpack4z-argonaut" % "0.1.3"
)

lazy val tests = Seq(
  "org.scalatest" %% "scalatest" % scalatestVersion,
  "org.scalacheck" %% "scalacheck" % scalacheckVersion,
  "com.github.alexarchambault" %% "argonaut-shapeless_6.1" % "0.3.1"
) map (_ % "test")

scalariformSettings

// wartremoverErrors ++= Warts.all

publishMavenStyle := true
publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
publishArtifact in Test := false
pomIncludeRepository := { _ => false }
