resolvers ++= Seq(
  Resolver.bintrayIvyRepo("ktosopl", "sbt-plugins"),
  Resolver.bintrayIvyRepo("sbt", "sbt-plugin-releases")
)

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-RC8")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.2.27")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.7.1")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.5")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.1")
