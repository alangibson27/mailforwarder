package com.socialthingy

import com.amazonaws.services.simpleemail.model.{Body, Content}

package object mailforwarder {
  import scala.language.implicitConversions
  implicit def stringToContent(s: String): Content = new Content().withData(s)
  implicit def stringToBody(s: String): Body = new Body().withText(s)

  type UserId = String
  type EmailAddress = String
}
