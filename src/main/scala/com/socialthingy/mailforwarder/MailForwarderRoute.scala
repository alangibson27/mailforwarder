package com.socialthingy.mailforwarder

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient
import com.amazonaws.services.simpleemail.model._

import scala.util.Try

trait MailForwarderRoute { this: MailLogging =>

  def emailClient: AmazonSimpleEmailServiceClient

  def sender: String

  def recipients: Map[UserId, EmailAddress]

  private val mandatoryFields = List("email", "name", "message")

  private val logRequestResultPrintln = DebuggingDirectives.logRequestResult(LoggingMagnet(_ => printRequestResult))

  val pingRoute: Route = get {
    path("ping") {
      complete("pong")
    }
  }

  val mailRoute: Route =
    logRequestResultPrintln {
      recipientFromPath { (recipient: EmailAddress) =>
        post {
          formFields("email", "name", "message") { (email, name, message) =>
            formFieldMap { allFields =>
              val additionalFields = allFields.filter(f => !mandatoryFields.contains(f._1)).map(f => s"${f._1}: ${f._2}")
              complete(sendMail(recipient, name, email, message, additionalFields))
            }
          }
        }
      }
    }

  private def sendMail(recipient: String,
                       name: String,
                       email: String,
                       message: String,
                       additionalFields: Iterable[String]) = Try {
    val body = s"""From: $name <$email>
               |${additionalFields.mkString("\n")}
               |$message""".stripMargin

    val emailMessage = new Message().withSubject(s"Message from $name").withBody(body)

    val emailRequest = new SendEmailRequest(
      sender,
      new Destination().
        withToAddresses(recipient),
      emailMessage
    )

    val result = emailClient.sendEmail(emailRequest)
    log.info(s"Sent message from $email for $recipient")
    result.getMessageId
  }

  private def recipientFromPath: Directive1[EmailAddress] = path("user" / Segment) flatMap {
    recipients.get(_) match {
      case Some(recipient) => provide(recipient)
      case None => complete(BadRequest)
    }
  }

  private def printRequestResult(req: HttpRequest)(res: Any): Unit = {
    log.debug(req.toString)
    log.debug(res.toString)
  }
}
