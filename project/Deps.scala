import sbt._

object Deps {
  val Ver = new {
    val cats             = "1.0.0-MF"
    val circe            = "0.9.0-M1"
    val scalacheck       = "1.13.5"
    val scalatest        = "3.0.3"
    val monix            = "2.3.0"
    val scalaLogging     = "3.5.0"
    val scalaJava8Compat = "0.8.0"
    val logback          = "1.2.3"
  }

  val Pkg = new {
    lazy val monixEval        = "io.monix"                   %% "monix-eval"         % Ver.monix
    lazy val monixReactive    = "io.monix"                   %% "monix-reactive"     % Ver.monix
    lazy val scalaLogging     = "com.typesafe.scala-logging" %% "scala-logging"      % Ver.scalaLogging
    lazy val scalaJava8Compat = "org.scala-lang.modules"     %% "scala-java8-compat" % Ver.scalaJava8Compat
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