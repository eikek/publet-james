package org.eknet.publet.james.mailets

import org.apache.james.transport.mailets.ToSenderFolder
import org.apache.mailet.{MailAddress, Mail}

/**
 * Overwriting [[org.apache.james.transport.mailets.ToSenderFolder]] to
 * suppress a NPE if getSender returns null.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 16:13
 */
class PubletToSenderFolder extends ToSenderFolder {

  override def service(mail: Mail) {
    if (mail.getSender == null) {
      val addr = new MailAddress(mail.getMessage.getSender.toString)
      //hack it into the mail object
      val field = mail.getClass.getDeclaredField("sender")
      if (field != null) {
        field.setAccessible(true)
        field.set(mail, addr)
      }
    }
    super.service(mail)
  }

}
