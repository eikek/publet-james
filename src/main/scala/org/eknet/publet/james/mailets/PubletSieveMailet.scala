/*
 * Copyright 2013 Eike Kettner
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

import com.google.inject.{Inject, Singleton}
import org.apache.jsieve.mailet.{ResourceLocator, SieveMailboxMailet, Poster}
import org.apache.mailet.{Mail, MailAddress}
import org.apache.james.user.api.UsersRepository

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 03.01.13 13:39
 */
@Singleton
class PubletSieveMailet @Inject() (poster: Poster, locator: ResourceLocator, userRepo: UsersRepository) extends SieveMailboxMailet {

  setPoster(poster)
  setLocator(locator)

  override def getUsername(m: MailAddress) = {
    if (userRepo.supportVirtualHosting()) {
      m.toString
    } else {
      super.getUsername(m)
    }
  }

  override def sieveMessage(recipient: MailAddress, aMail: Mail) {
    if (aMail.getRecipients.size() > 1) {
      //the FileInto action expects exactly one recipient. This method is invoked
      //for each recipient in the mail. so it is safe to override the recipients
      //here with the single one from the arguments. The next calls are just about
      //to save the mail.
      aMail.setRecipients(java.util.Arrays.asList(recipient))
    }
    super.sieveMessage(recipient, aMail)
  }

}
