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

package org.eknet.publet.james.data

import org.apache.james.queue.api.ManageableMailQueue
import org.apache.james.queue.api.ManageableMailQueue.{MailQueueItemView, MailQueueIterator, Type}
import org.apache.mailet.Mail
import java.util.concurrent.{Executors, LinkedBlockingQueue, TimeUnit}
import org.apache.james.queue.api.MailQueue.{MailQueueException, MailQueueItem}
import org.apache.james.lifecycle.api.LifecycleUtil
import java.io.File
import grizzled.slf4j.Logging
import org.eknet.publet.ext.jmx.JmxService
import java.util.Hashtable
import javax.management.ObjectName
import annotation.tailrec

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.12.12 02:17
 */
class FileMailQ(parentDir: File, queueName: String, sync: Boolean) extends ManageableMailQueue with Logging with MailQMBean {

  private val NEXT_DELIVERY = "FileQueueNextDelivery"
  private val scheduler = Executors.newSingleThreadScheduledExecutor()

  private val queue = {
    val q = new SpoolQueue
    //schedule existing mails
    q.flush(schedule = true)
    info("["+queueName+"] Loaded '"+q.size+"' items in queue.")
    q
  }
  registerMBean()

  private[this] def registerMBean() {
    val table = new Hashtable[String, String]()
    table.put("type", "FileMailQueue")
    table.put("name", queueName)
    val name = ObjectName.getInstance("org.eknet.publet.james", table)
    JmxService.defaultMBeanServer.registerMBean(this, name)
  }

  private def tryCatch[A](body: => A): A = {
    try {
      body
    } catch {
      case ie: InterruptedException => Thread.currentThread().interrupt(); throw new MailQueueException("Interrupted", ie)
      case e: Exception => throw new MailQueueException("Error", e)
    }
  }

  def enQueue(mail: Mail) {
    enQueue(mail, 0, TimeUnit.SECONDS)
  }

  def enQueue(mail: Mail, delay: Long, unit: TimeUnit) {
    tryCatch {
      mail.setAttribute(NEXT_DELIVERY, System.currentTimeMillis()+TimeUnit.MILLISECONDS.convert(delay, unit))
      val key = queue.spool.put(mail)
      val cmd = new Runnable {
        def run() { queue.offer(key) }
      }
      if (delay <= 0) {
        cmd.run()
      } else {
        scheduler.schedule(cmd, delay, unit)
      }
    }
  }

  def deQueue() = tryCatch {
    val handle = queue.take
    new MailQueueItem {
      def done(success: Boolean) {
        if (success) queue.commit(handle) else queue.rollback(handle)
      }
      def getMail = handle.mail
    }
  }

  def getSize = tryCatch(queue.spool.size)
  def flush() = tryCatch(queue.flush(schedule = false))
  def clear() = tryCatch(queue.spool.clear())
  def remove(`type`: Type, value: String) = tryCatch {
    if (`type` == Type.Name) {
      if (queue.spool.remove(value)) 1 else 0
    } else {
      sys.error("Type '"+ `type` +" not supported")
    }
  }

  def browse() = tryCatch {
    new MailQueueIterator {
      val delegate = queue.spool.all.iterator
      def next() = MailItemView(delegate.next().mail)
      def remove() { throw new UnsupportedOperationException() }
      def hasNext = delegate.hasNext
      def close() {}
    }
  }


  def remove(name: String) {
    remove(Type.Name, name)
  }

  def list = {
    import collection.JavaConversions._
    browse().take(50).map(_.toString).toArray
  }

  final case class MailItemView(mail: Mail) extends MailQueueItemView {
    def getMail = mail
    def getNextDelivery =
      Option(mail.getAttribute(NEXT_DELIVERY)).map(_.asInstanceOf[Long]).getOrElse(0L)

    override def toString = {
      import collection.JavaConversions._
      mail.getName+"; from=" +
        mail.getSender+"; to=" +
        mail.getRecipients.mkString(", ") + "; remoteHost="+
        mail.getRemoteHost
    }
  }

  final class SpoolQueue {
    private val handleQueue = new LinkedBlockingQueue[String]()
    val spool = new FileSpool(new File(parentDir, queueName), sync)

    def size = handleQueue.size()

    @tailrec
    def take: FileSpool#MailHandle = {
      val key = handleQueue.take()
      spool.get(key) match {
        case Some(el) => el
        case None => take
      }
    }

    def put(mail: Mail) {
      val key = spool.put(mail)
      if (!handleQueue.contains(key)) {
        handleQueue.put(key)
      }
    }

    def offer(key: String) {
      if (!handleQueue.contains(key)) {
        handleQueue.offer(key)
      }
    }
    def rollback(handle: FileSpool#MailHandle) {
      handleQueue.offer(handle.key)
      LifecycleUtil.dispose(handle.mail)
    }
    def commit(handle: FileSpool#MailHandle) {
      spool.remove(handle.key)
      LifecycleUtil.dispose(handle.mail)
    }
    def flush(schedule: Boolean) = {
      spool.all.map(mh => {
        val cmd = new Runnable {
          def run() { offer(mh.key) }
        }
        val next = MailItemView(mh.mail).getNextDelivery
        if (next > System.currentTimeMillis() && schedule) {
          val delay = next - System.currentTimeMillis()
          scheduler.schedule(cmd, delay, TimeUnit.MILLISECONDS)
        } else {
          cmd.run()
        }
        1
      }).foldLeft(0)(_ + _)
    }
  }
}