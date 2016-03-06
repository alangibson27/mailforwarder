package com.socialthingy.mailforwarder

import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError, OK}
import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsyncClient
import com.amazonaws.services.simpleemail.model._
import org.scalatest.{Matchers, FlatSpec}
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._

class MailForwarderRouteSpec extends FlatSpec with Matchers with MockitoSugar with MailForwarderRoute with ScalatestRouteTest {
  override val emailClient = mock[AmazonSimpleEmailServiceAsyncClient]

  override val sender = "admin@socialthingy.com"
  override val recipients = Map(
    "12345" -> "12345@socialthingy.com",
    "54321" -> "54321@socialthingy.com"
  )

  val formData = Map(
    "email" -> "alangibson27@gmail.com",
    "name" -> "Alan",
    "message" -> "hello",
    "extra" -> "here"
  )

  "mail forwarder" should "send an email to the supplied address" in {
    val expectedBody = """From: Alan <alangibson27@gmail.com>
                         |extra: here
                         |hello""".stripMargin
    val expectedRequest = new SendEmailRequest(
      sender,
      new Destination().withToAddresses("12345@socialthingy.com"),
      new Message().withSubject("Message from Alan").withBody(expectedBody)
    )

    when(emailClient.sendEmail(expectedRequest)).thenReturn(new SendEmailResult().withMessageId("123"))
    
    Post("/user/12345", FormData(formData)) ~> mailRoute ~> check {
      status shouldBe OK
      entityAs[String] shouldBe "123"
    }
  }

  it should "send to the correct recipient based on the ID in the path" in {
    val expectedBody = """From: Alan <alangibson27@gmail.com>
                         |extra: here
                         |hello""".stripMargin
    val expectedRequest = new SendEmailRequest(
      sender,
      new Destination().withToAddresses("54321@socialthingy.com"),
      new Message().withSubject("Message from Alan").withBody(expectedBody)
    )

    when(emailClient.sendEmail(expectedRequest)).thenReturn(new SendEmailResult().withMessageId("123"))

    Post("/user/54321", FormData(formData)) ~> mailRoute ~> check {
      status shouldBe OK
      entityAs[String] shouldBe "123"
    }
  }

  it should "handle multibyte characters correctly" in {
    val multibyteFormData = Map(
      "email" -> "alangibson27@gmail.com",
      "name" -> "アラン",
      "message" -> "こんにちは",
      "extra" -> "こちら"
    )

    val expectedBody = """From: アラン <alangibson27@gmail.com>
                         |extra: こちら
                         |こんにちは""".stripMargin
    val expectedRequest = new SendEmailRequest(
      sender,
      new Destination().withToAddresses("54321@socialthingy.com"),
      new Message().withSubject("Message from アラン").withBody(expectedBody)
    )

    when(emailClient.sendEmail(expectedRequest)).thenReturn(new SendEmailResult().withMessageId("123"))

    Post("/user/54321", FormData(multibyteFormData)) ~> mailRoute ~> check {
      status shouldBe OK
      entityAs[String] shouldBe "123"
    }
  }

  it should "return a 500 error if sending email fails" in {
    when(emailClient.sendEmail(any[SendEmailRequest])).thenThrow(new MessageRejectedException("error"))
    
    Post("/user/12345", FormData(formData)) ~> mailRoute ~> check {
      status shouldBe InternalServerError
    }
  }

  it should "return a 400 error if the user ID is not recognised" in {
    Post("/user/notfound", FormData(formData)) ~> mailRoute ~> check {
      status shouldBe BadRequest
    }
  }
}
