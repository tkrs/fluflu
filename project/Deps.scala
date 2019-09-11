import sbt._

object Deps {
  val Ver = new {
    val `scala2.12`   = "2.12.10"
    val `scala2.13`   = "2.13.0"
    val mess          = "0.0.12"
    val scalacheck    = "1.14.0"
    val scalatest     = "3.0.8"
    val scalaLogging  = "3.9.2"
    val logback       = "1.2.3"
    val msgpackJava   = "0.8.17"
    val mockito       = "2.23.0"
    val kindProjector = "0.10.3"
  }

  def is2_13(v: String): Boolean = CrossVersion.partialVersion(v) match {
    case Some((2, 13)) => true
    case _             => false
  }

  val Pkg = new {
    lazy val mess           = "com.github.tkrs"            %% "mess-core"      % Ver.mess
    lazy val scalaLogging   = "com.typesafe.scala-logging" %% "scala-logging"  % Ver.scalaLogging
    lazy val msgpackJava    = "org.msgpack"                % "msgpack-core"    % Ver.msgpackJava
    lazy val logbackClassic = "ch.qos.logback"             % "logback-classic" % Ver.logback
    lazy val mockito        = "org.mockito"                % "mockito-core"    % Ver.mockito
    lazy val scalacheck     = "org.scalacheck"             %% "scalacheck"     % Ver.scalacheck
    lazy val scalatest      = "org.scalatest"              %% "scalatest"      % Ver.scalatest
    lazy val kindProjector  = "org.typelevel"              %% "kind-projector" % Ver.kindProjector

    lazy val forTest = Seq(scalatest, scalacheck, mockito).map(_ % Test)
  }
}
