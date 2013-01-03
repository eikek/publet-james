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

import org.apache.jsieve.mailet.Poster
import javax.mail.internet.MimeMessage
import com.google.inject.{Inject, Singleton}
import org.apache.james.user.api.UsersRepository
import org.apache.james.mailbox.{MailboxSession, MailboxManager}
import java.net.URI
import javax.mail.MessagingException
import grizzled.slf4j.Logging
import org.apache.james.mailbox.model.{MailboxConstants, MailboxPath}
import java.util.Date
import org.apache.james.core.MimeMessageInputStream

/**
 * This is a modified version of the implementation in [[org.apache.james.transport.mailets.SieveMailet]]
 * trying to solve some problems found in there.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 03.01.13 13:06
 */
@Singleton
class MailPoster @Inject() (userRepo: UsersRepository, mboxman: MailboxManager) extends Poster {
  //url: mailbox://eike@localhost/INBOX/work

  private val handlers = Map(
    MailboxHandler.scheme -> MailboxHandler
  )

  def post(uriStr: String, mail: MimeMessage) {
    val uri = new URI(uriStr)
    val handler = handlers.get(uri.getScheme)
      .getOrElse(throw new MessagingException("Scheme not supported: "+ uri.getScheme))

    handler.handle(uri, mail)
  }

  trait PostHandler extends Logging {
    def doHandle(uri: URI, mail: MimeMessage)
    def scheme: String

    def handle(uri: URI, mail: MimeMessage) {
      try {
        doHandle(uri, mail)
      } catch {
        case e: MessagingException => throw e
        case e: Exception => throw new MessagingException("Error while handling message.", e)
      }
    }

    def withSession[A](user: String)(body: MailboxSession => A): A = {
      val session = mboxman.createSystemSession(user, logger.logger)
      try {
        mboxman.startProcessingRequest(session)
        body(session)
      } finally {
        session.close()
        try {
          mboxman.logout(session, true)
        } finally {
          mboxman.endProcessingRequest(session)
        }
      }
    }
  }

  object MailboxHandler extends PostHandler {
    val scheme = "mailbox"

    def doHandle(uri: URI, mail: MimeMessage) {
      val user = getUser(uri)
      withSession(user)(session => {
        val destination = getDestination(uri, session)
        val path = new MailboxPath(MailboxConstants.USER_NAMESPACE, user, destination)
        if (destination == "INBOX" && !mboxman.mailboxExists(path, session)) {
          mboxman.createMailbox(path, session)
        }
        val mailbox = mboxman.getMailbox(path, session)
        mailbox.appendMessage(new MimeMessageInputStream(mail), new Date(), session, true, null)
      })
    }

    def getUser(uri: URI) =
      if (userRepo.supportVirtualHosting()) uri.getUserInfo+"@"+uri.getHost else uri.getUserInfo

    def getDestination(uri: URI, session: MailboxSession) = {
      if (uri.getPath == null || uri.getPath.isEmpty) {
        "INBOX"
      } else {
        uri.getPath.dropWhile(c => c == '/').replace("/", session.getPathDelimiter.toString)
      }
    }
  }
}
