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

package org.eknet.publet.james.guice

import org.apache.james.mailetcontainer.api.MailetLoader
import org.apache.mailet.{Mailet, MailetConfig}
import com.google.inject.{Injector, Inject, Singleton}
import javax.mail.MessagingException
import javax.mail

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 23.10.12 21:00
 */
@Singleton
class GuiceMailetLoader @Inject() (injector: Injector) extends MailetLoader {

  def getMailet(config: MailetConfig) = {
    val name = config.getMailetName match {
      case x if (x.indexOf('.') < 1) => standardPackage +"."+ x
      case x => x
    }
    try {
      val clazz = getClass.getClassLoader.loadClass(name)
      val inst = injector.getInstance(clazz).asInstanceOf[Mailet]
      inst.init(config)
      inst
    }
    catch {
      case e: MessagingException => throw e
      case e: Exception => throw new mail.MessagingException("Unable to load mailet: "+ config.getMailetName, e)
    }
  }

  def standardPackage = "org.apache.james.transport.mailets"
}
