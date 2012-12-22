/*
 * Copyright 2012 Eike Kettner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eknet.publet.james.ui

import org.eknet.publet.engine.scala.ScalaScript
import org.eknet.publet.web.util.PubletWeb
import org.apache.james.queue.api.{ManageableMailQueue, MailQueueFactory}
import org.apache.mailet.MailAddress
import org.eknet.publet.james.data.FileMailQFactory

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.12.12 00:41
 */
class ManageSpool extends ScalaScript {
  import org.eknet.publet.web.util.RenderUtils.makeJson

  def serve() = (paramLc(actionParam), param("queue")) match {
    case (Some("getsize"), Some(queue)) => getSize(queue)
    case (Some("flush"), Some(queue)) => flushQueue(queue)
    case (Some("clear"), Some(queue)) => clearQueue(queue)
    case (Some("remove"), Some(queue)) => remove(queue)
    case (Some("list"), Some(queue)) => listQueue(queue)
    case c@_ => failure("Unknown command: "+ c)
  }

  private def listQueue(name: String) = withPerm("james:spool:list") {
    import collection.JavaConversions._
    val queue = getQueue(name)
    val iter = queue.browse()
    val items = iter.take(100).toList
    iter.close()

    def toJson(item: ManageableMailQueue.MailQueueItemView) = {
      val mail = item.getMail
      Map(
        "next" -> nextDeliveryOffset(item.getNextDelivery),
        "name" -> mail.getName,
        "state" -> mail.getState,
        "sender" -> mail.getSender.toString,
        "recipients" -> mail.getRecipients.collect({case x:MailAddress=>x.toString}).mkString(", "),
        "remoteAddress" -> mail.getRemoteAddr,
        "remoteHost" -> mail.getRemoteHost
      )
    }
    val resp = Map(
      "mailItems" -> (items map toJson),
      "size" -> queue.getSize,
      "queueNames" -> getQueueNames.toArray,
      "currentQueue" -> name
    )
    makeJson( resp )
  }

  private def nextDeliveryOffset(ts: Long) = {
    val now = System.currentTimeMillis()
    if (ts <= now) {
      "Now"
    } else {
      "~"+TimeSpan.fromNow(ts).humanString
    }
  }

  private def getSize(name: String) = withPerm("james:spool:getsize") {
    val spool = getQueue(name)
    makeJson(spool.getSize)
  }

  private def flushQueue(name: String) = withPerm("james:spool:flush") {
    val c = getQueue(name).flush()
    success("Flushed "+ c +" mails from "+name+".")
  }

  private def clearQueue(name: String) = withPerm("james:spool:remove") {
    val c = getQueue(name).clear()
    success("Removed "+ c +" mails from "+name+".")
  }

  private def remove(name: String) = withPerm("james:spool:remove") {
    val spool = getQueue(name)
    param("name") match {
      case Some(x) => {
        val c = spool.remove(ManageableMailQueue.Type.Name, x)
        success("Removed "+ c +" mails from "+name+".")
      }
      case _ => failure("Name parameter missing.")
    }
  }

  private[this] def getQueue(name: String) = {
    PubletWeb.instance[MailQueueFactory].get.getQueue(name) match {
      case x: ManageableMailQueue => x
      case _ => throw new IllegalStateException("Managing Queues not supported.")
    }
  }
  private def getQueueNames = PubletWeb.instance[MailQueueFactory].get.asInstanceOf[FileMailQFactory].queueNames
}
