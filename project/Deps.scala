import sbt._

object Deps {
  val Ver = new {
    val `scala2.11`   = "2.11.12"
    val `scala2.12`   = "2.12.8"
    val `scala2.13`   = "2.13.0-M5"
    val scalafmt      = "1.5.1"
    val shapeless     = "2.3.3"
    val mess          = "0.0.11"
    val scalacheck    = "1.14.0"
    val scalatest     = "3.0.5"
    val scalatestSnap = "3.0.6-SNAP5"
    val scalaLogging  = "3.9.0"
    val logback       = "1.2.3"
    val msgpackJava   = "0.8.16"
    val mockito       = "2.23.0"
    val kindProjector = "0.9.9"
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

    def scalatest(scalaVersion: String) = "org.scalatest" %% "scalatest" % {
      if (is2_13(scalaVersion)) Ver.scalatestSnap else Ver.scalatest
    }

    def forTest(scalaVersion: String) = Seq(scalatest(scalaVersion), scalacheck, mockito).map(_ % "test")
  }
}
