lazy val akkaHttpVersion = "10.2.2"
lazy val akkaVersion = "2.6.10"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.13.3"
    )),
    name := "weather-forecast",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "2.0.2",
      "com.typesafe.slick" %% "slick" % "3.3.2",
      "org.xerial" % "sqlite-jdbc" % "3.7.2",
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.0.8" % Test
    )
  )
