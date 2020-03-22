import sbt._

object Deps {
  val Ver = new {
    val `scala2.12` = "2.12.11"
    val `scala2.13` = "2.13.1"

    val msgpackJava  = "0.8.20"
    val mess         = "0.2.1"
    val scalaLogging = "3.9.2"
    val logback      = "1.2.3"

    val scalatest = "3.1.0"
    val scalatestplus = new {
      val scalacheck = "3.1.0.1"
      val mockito    = "3.1.0.0"
    }
  }

  def is2_13(v: String): Boolean = CrossVersion.partialVersion(v) match {
    case Some((2, 13)) => true
    case _             => false
  }

  val Pkg = new {
    lazy val msgpackJava    = "org.msgpack"                % "msgpack-core"    % Ver.msgpackJava
    lazy val mess           = "com.github.tkrs"            %% "mess-core"      % Ver.mess
    lazy val scalaLogging   = "com.typesafe.scala-logging" %% "scala-logging"  % Ver.scalaLogging
    lazy val logbackClassic = "ch.qos.logback"             % "logback-classic" % Ver.logback

    lazy val scalatest  = "org.scalatest"     %% "scalatest"       % Ver.scalatest
    lazy val scalacheck = "org.scalatestplus" %% "scalacheck-1-14" % Ver.scalatestplus.scalacheck
    lazy val mockito    = "org.scalatestplus" %% "mockito-3-2"     % Ver.scalatestplus.mockito

    lazy val forTest = Seq(scalatest, scalacheck, mockito).map(_ % Test)
  }
}
