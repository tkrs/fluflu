import sbt._

object Deps {
  val Ver = new {
    val `scala2.11`   = "2.11.12"
    val `scala2.12`   = "2.12.6"
    val `scala2.13`   = "2.13.0-M4"
    val scalafmt      = "1.5.1"
    val shapeless     = "2.3.3"
    val mess          = "0.0.8"
    val circe         = "0.10.0"
    val circeM        = "0.10.0"
    val scalacheck    = "1.14.0"
    val scalatest     = "3.0.5"
    val scalatestSnap = "3.0.6-SNAP2"
    val monix         = "3.0.0-RC1"
    val scalaLogging  = "3.9.0"
    val logback       = "1.2.3"
    val msgpackJava   = "0.8.16"
    val mockito       = "2.22.0"
    val kindProjector = "0.9.7"
  }

  def is2_13(v: String): Boolean = CrossVersion.partialVersion(v) match {
    case Some((2, 13)) => true
    case _             => false
  }

  val Pkg = new {
    lazy val mess           = "com.github.tkrs"            %% "mess-core"      % Ver.mess
    lazy val scalaLogging   = "com.typesafe.scala-logging" %% "scala-logging"  % Ver.scalaLogging
    lazy val scalacheck     = "org.scalacheck"             %% "scalacheck"     % Ver.scalacheck
    lazy val msgpackJava    = "org.msgpack"                % "msgpack-core"    % Ver.msgpackJava
    lazy val logbackClassic = "ch.qos.logback"             % "logback-classic" % Ver.logback
    lazy val mockito        = "org.mockito"                % "mockito-core"    % Ver.mockito
    lazy val kindProjector  = "org.spire-math"             %% "kind-projector" % Ver.kindProjector
    lazy val monixReactive  = "io.monix"                   %% "monix-reactive" % Ver.monix

    def circe(scalaVersion: String) =
      Seq(
        "io.circe" %% "circe-core",
        "io.circe" %% "circe-generic",
        "io.circe" %% "circe-parser"
      ).map(_ % {
        if (is2_13(scalaVersion)) Ver.circeM else Ver.circe
      })

    def scalatest(scalaVersion: String) = "org.scalatest" %% "scalatest" % {
      if (is2_13(scalaVersion)) Ver.scalatestSnap else Ver.scalatest
    }

    def forTest(scalaVersion: String) = Seq(scalatest(scalaVersion), scalacheck, mockito).map(_ % "test")
  }
}
