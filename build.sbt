name := "fluflu"

organization := "com.github.tkrs"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.7"

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

scalacOptions := Seq(
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
  "-Xlint"
)

resolvers += Resolver.sonatypeRepo("snapshots")

lazy val scalazVersion = "7.1.3"
lazy val scalacheckVersion = "1.12.3"
lazy val scalatestVersion = "2.2.5"

lazy val scalaz = Seq(
  "org.scalaz" %% "scalaz-core" % scalazVersion,
  "org.scalaz" %% "scalaz-concurrent" % scalazVersion,
  "org.scalaz" %% "scalaz-scalacheck-binding" % scalazVersion % "test"
)

lazy val others = Seq(
  "io.argonaut" %% "argonaut" % "6.0.4",
  "com.github.alexarchambault" %% "argonaut-shapeless_6.1" % "0.3.1",
  "com.github.xuwei-k" % "msgpack4z-api" % "0.1.0",
  "com.github.xuwei-k" %% "msgpack4z-core" % "0.1.4",
  "com.github.xuwei-k" %% "msgpack4z-native" % "0.1.1",
  "com.github.xuwei-k" %% "msgpack4z-argonaut" % "0.1.3"
)

lazy val test = Seq(
  "org.scalatest" %% "scalatest" % scalatestVersion,
  "org.scalacheck" %% "scalacheck" % scalacheckVersion
) map (_ % "test")

lazy val deps = (scalaz ++ others ++ test) map (_.withSources())

libraryDependencies ++= deps

// lazy val commonSettings = Seq(
//   scalaVersion := "2.11.7",
//   organization := "com.github.tkrs"
// )
// lazy val specs2core = "org.specs2" %% "specs2-core" % "2.4.14"
//
// lazy val root = (project in file(".")).
//   configs(IntegrationTest).
//   settings(commonSettings: _*).
//   settings(Defaults.itSettings: _*).
//   settings(
//     libraryDependencies += specs2core % "it,test"
//   )

scalariformSettings

// wartremoverErrors in (Compile, compile) ++= Warts.all
