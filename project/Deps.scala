import sbt._

object Deps {
  val Ver = new {
    val `scala2.12`      = "2.12.4"
    val `scala2.11`      = "2.11.11"
    val scalafmt         = "1.2.0"
    val cats             = "1.0.0-RC1"
    val circe            = "0.9.0-M2"
    val scalacheck       = "1.13.5"
    val scalatest        = "3.0.3"
    val monix            = "2.3.0"
    val scalaLogging     = "3.5.0"
    val logback          = "1.2.3"
    val msgpackJava      = "0.8.13"
  }

  val Pkg = new {
    lazy val monixEval        = "io.monix"                   %% "monix-eval"         % Ver.monix
    lazy val monixReactive    = "io.monix"                   %% "monix-reactive"     % Ver.monix
    lazy val scalaLogging     = "com.typesafe.scala-logging" %% "scala-logging"      % Ver.scalaLogging
    lazy val msgpackJava      = "org.msgpack"                % "msgpack-core"        % Ver.msgpackJava
    lazy val circeCore        = "io.circe"                   %% "circe-core"         % Ver.circe
    lazy val circeGeneric     = "io.circe"                   %% "circe-generic"      % Ver.circe
    lazy val circeParser      = "io.circe"                   %% "circe-parser"       % Ver.circe
    lazy val logbackClassic   = "ch.qos.logback"             % "logback-classic"     % Ver.logback
    lazy val catsCore         = "org.typelevel"              %% "cats-core"          % Ver.cats
    lazy val scalatest        = "org.scalatest"              %% "scalatest"          % Ver.scalatest
    lazy val scalacheck       = "org.scalacheck"             %% "scalacheck"         % Ver.scalacheck

    lazy val forTest = Seq(catsCore, scalatest, scalacheck).map(_ % "test")
  }
}
