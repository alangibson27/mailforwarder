name := "mailforwarder"

version := "1.0.0"

scalaVersion := "2.11.7"

libraryDependencies ++= {
  val appDependencies = Seq(
    "com.amazonaws" % "aws-java-sdk-ses" % "1.10.55",
    "com.typesafe.akka" %% "akka-http-experimental" % "2.4.2",
    "ch.qos.logback" % "logback-classic" % "1.1.6",
    "com.typesafe.akka" % "akka-slf4j_2.11" % "2.4.2"
  )

  val testDependencies = Seq(
    "org.mockito" % "mockito-all" % "1.10.19",
    "org.scalatest" %% "scalatest" % "2.2.6" % "test",
    "com.typesafe.akka" %% "akka-http-testkit" % "2.4.2"
  )

  appDependencies ++ testDependencies
}

enablePlugins(SbtNativePackager, RpmPlugin)

maintainer in Linux := "Alan Gibson <alangibson27@gmail.com>"

packageSummary in Linux := "Mail forwarder"

packageDescription in Linux := "Sends email via Amazon SES"

daemonUser in Linux := "mailforwarder"

daemonGroup in Linux := (daemonUser in Linux).value

linuxPackageMappings += {
  val logrotateFile = sourceDirectory.value / "main"/ "resources" / "etc" / "logrotate.d" / "mailforwarder"
  packageMapping(logrotateFile -> "/etc/logrotate.d/mailforwarder")
}

rpmVendor := "com.socialthingy"

packageName in Rpm := "mailforwarder"
