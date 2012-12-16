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

import org.apache.mailet.base.GenericMailet
import org.apache.mailet.Mail
import org.eknet.publet.web.util.PubletWeb
import com.google.common.eventbus.EventBus
import grizzled.slf4j.Logging
import com.google.inject.{Inject, Singleton}

/**
 * This mailet posts the mail on publet's global
 * event bus. That way other components can just
 * subscribe to that event in order to get notified
 * of mails while they're also able to modify them.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 16.12.12 17:15
 */
@Singleton
class EventBusMailet @Inject() (bus: EventBus) extends GenericMailet {

  def service(mail: Mail) {
    bus.post(new IncomeMailEvent(mail, getMailetConfig))
  }

  override def getMailetInfo = {
    "EventBusMailet. Integrates into publet's event system."
  }

}
