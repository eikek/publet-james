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

import org.apache.james.queue.api.{MailQueue, MailQueueFactory}
import com.google.inject.{Inject, Singleton}
import org.apache.james.filesystem.api.FileSystem
import java.util.concurrent.ConcurrentHashMap
import grizzled.slf4j.Logging
import com.google.common.base.{Suppliers, Supplier}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.12.12 01:45
 */
@Singleton
class FileMailQFactory @Inject()(fs: FileSystem) extends MailQueueFactory with Logging {

  private val queues = new ConcurrentHashMap[String, Supplier[MailQueue]]()

  var sync: Boolean = true

  def getQueue(name: String) = {
    val f = Suppliers.memoize(createQueue(name))
    val cached = queues.putIfAbsent(name, f)
    if (cached != null) cached.get() else f.get()
  }

  def queueNames = {
    import collection.JavaConversions._
    queues.keySet().toSet
  }

  private def createQueue(name: String) = new Supplier[MailQueue] {
    def get() = new FileMailQ(fs.getFile("file://var/store/queue"), name, sync)
  }
}
