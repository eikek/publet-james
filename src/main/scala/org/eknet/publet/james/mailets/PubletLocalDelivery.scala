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

package org.eknet.publet.james.mailets

import org.apache.james.transport.mailets.{SieveMailet, LocalDelivery}
import com.google.inject.{Inject, Singleton}
import org.apache.jsieve.mailet.ResourceLocator
import org.apache.mailet.base.GenericMailet
import org.apache.mailet.{MailetConfig, Mail}
import org.apache.james.user.api.UsersRepository
import org.apache.james.mailbox.MailboxManager

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 25.12.12 17:48
 */
@Singleton
class PubletLocalDelivery @Inject() (sieveMailet: PubletSieveMailet) extends GenericMailet {

  def service(mail: Mail) {
    if (mail.getState != Mail.GHOST) {
      sieveMailet.service(mail)
    }
  }

  override def init() {
    super.init()
    sieveMailet.init(extend(Map("addDeliveryHeader" -> "Delivered-To", "resetReturnPath" -> "true")))
    sieveMailet.setQuiet(getInitParameter("quiet", true))
  }

  override def getMailetInfo = "Publet's Local Delivery Mailet"

  private def extend(map: Map[String, String]) = new ExtendedConfig(getMailetConfig, map)

  private final class ExtendedConfig(delegate: MailetConfig, map: Map[String, String]) extends MailetConfig {

    def getInitParameter(name: String) = map.get(name).getOrElse(delegate.getInitParameter(name))

    def getInitParameterNames = {
      import collection.JavaConversions._
      map.keySet.iterator ++ delegate.getInitParameterNames
    }
    def getMailetContext = delegate.getMailetContext
    def getMailetName = delegate.getMailetName
  }
}
