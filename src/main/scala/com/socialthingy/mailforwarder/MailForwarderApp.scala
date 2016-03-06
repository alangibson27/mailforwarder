package com.socialthingy.mailforwarder

import akka.actor.ActorSystem
import akka.event.{LogSource, Logging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.amazonaws.regions.{Regions, Region}
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient
import com.typesafe.config.ConfigFactory

object MailForwarderApp extends App with MailForwarderRoute with MailLogging {

  val config = ConfigFactory.load.withFallback(ConfigFactory.systemEnvironment())

  implicit val system = ActorSystem("my-system", config)
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  override val emailClient: AmazonSimpleEmailServiceClient = {
    val client = new AmazonSimpleEmailServiceClient()
    client.setRegion(Region.getRegion(Regions.US_EAST_1))
    client
  }

  override val recipients: Map[UserId, EmailAddress] = config.getString("RECIPIENTS").split(",").map {
    nv => nv.split(":") match {
      case Array(userId, email) => userId -> email
    }
  }.toMap

  log.info(s"Starting application with recipients ${recipients.values.mkString(",")}")

  override val sender: String = config.getString("SENDER_ADDRESS")

  Http().bindAndHandle(pingRoute ~ mailRoute, "localhost", 8765)

  println(
    """
      |  __  __       _ _   ______                               _
      | |  \/  |     (_) | |  ____|                             | |
      | | \  / | __ _ _| | | |__ ___  _ ____      ____ _ _ __ __| | ___ _ __
      | | |\/| |/ _` | | | |  __/ _ \| '__\ \ /\ / / _` | '__/ _` |/ _ \ '__|
      | | |  | | (_| | | | | | | (_) | |   \ V  V / (_| | | | (_| |  __/ |
      | |_|  |_|\__,_|_|_| |_|  \___/|_|    \_/\_/ \__,_|_|  \__,_|\___|_|
      |
    """.stripMargin)

}
