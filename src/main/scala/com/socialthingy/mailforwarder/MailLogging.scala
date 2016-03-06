package com.socialthingy.mailforwarder

import akka.actor.ActorSystem
import akka.event.{Logging, LogSource}

trait MailLogging {

  def system: ActorSystem

  implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
    def genString(o: AnyRef): String = o.getClass.getName
    override def getClazz(o: AnyRef): Class[_] = o.getClass
  }

  lazy val log = Logging(system, this)

}
