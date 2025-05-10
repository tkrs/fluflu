import sbt._

object Dependencies {
  val Ver = new {
    val scala2 = "2.13.16"
    val scala3 = "3.7.0"

    val msgpackJava  = "0.9.9"
    val mess         = "0.3.6"
    val scalaLogging = "3.9.5"
    val logback      = "1.2.3"

    val scalatest = "3.2.19"
    val scalatestplus = new {
      val scalacheck = "3.2.19.0"
      val mockito    = "3.2.19.0"
    }
  }

  val Pkg = new {
    lazy val msgpackJava    = "org.msgpack"                 % "msgpack-core"    % Ver.msgpackJava
    lazy val mess           = "com.github.tkrs"            %% "mess-core"       % Ver.mess
    lazy val scalaLogging   = "com.typesafe.scala-logging" %% "scala-logging"   % Ver.scalaLogging
    lazy val logbackClassic = "ch.qos.logback"              % "logback-classic" % Ver.logback

    lazy val scalatest  = "org.scalatest"     %% "scalatest"       % Ver.scalatest                % Test
    lazy val scalacheck = "org.scalatestplus" %% "scalacheck-1-18" % Ver.scalatestplus.scalacheck % Test
    lazy val mockito    = "org.scalatestplus" %% "mockito-5-12"    % Ver.scalatestplus.mockito    % Test
  }
}
