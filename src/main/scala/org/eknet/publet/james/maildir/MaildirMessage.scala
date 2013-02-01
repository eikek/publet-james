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

package org.eknet.publet.james.maildir

import lib.MessageName
import org.apache.james.mailbox.store.mail.model.{Message, AbstractMessage}
import javax.mail.Flags
import java.nio.file.Path
import javax.mail.util.SharedFileInputStream
import org.apache.james.mime4j.stream.{EntityState, RecursionMode, MimeTokenStream, MimeConfig}
import org.apache.james.mime4j.message.{MaximalBodyDescriptor, DefaultBodyDescriptorBuilder}
import org.apache.james.mailbox.store.streaming.CountingInputStream
import org.apache.commons.io.IOUtils
import java.io.{InputStream, PushbackInputStream}
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 31.01.13 12:42
 */
class MaildirMessage(uid: Long, name: MessageName, file: Path) extends Message[Int] {

  private val propertyBuilder = new PropertyBuilder()
  private var modseq: Long = _
  private var bodyStartOctet: Int = _

  private lazy val message = {
    val tmpMsgIn = new SharedFileInputStream(file.toFile)
    bodyStartOctet = getBodyStartOctet(tmpMsgIn)

    // Disable line length... This should be handled by the smtp server
    // component and not the parser itself
    // https://issues.apache.org/jira/browse/IMAP-122
    val config: MimeConfig = new MimeConfig
    config.setMaxLineLen(-1)
    val parser: MimeTokenStream = new MimeTokenStream(config, new DefaultBodyDescriptorBuilder)
    parser.setRecursionMode(RecursionMode.M_NO_RECURSE)
    parser.parse(tmpMsgIn.newStream(0, -1))
    var next: EntityState = parser.next

    while (next != EntityState.T_BODY && next != EntityState.T_END_OF_STREAM && next != EntityState.T_START_MULTIPART) {
      next = parser.next
    }
    val descriptor: MaximalBodyDescriptor = parser.getBodyDescriptor.asInstanceOf[MaximalBodyDescriptor]
    var mediaType: String = null
    val mediaTypeFromHeader: String = descriptor.getMediaType
    var subType: String = null
    if (mediaTypeFromHeader == null) {
      mediaType = "text"
      subType = "plain"
    }
    else {
      mediaType = mediaTypeFromHeader
      subType = descriptor.getSubType
    }
    propertyBuilder.setMediaType(mediaType)
    propertyBuilder.setSubType(subType)
    propertyBuilder.setContentID(descriptor.getContentId)
    propertyBuilder.setContentDescription(descriptor.getContentDescription)
    propertyBuilder.setContentLocation(descriptor.getContentLocation)
    propertyBuilder.setContentMD5(descriptor.getContentMD5Raw)
    propertyBuilder.setContentTransferEncoding(descriptor.getTransferEncoding)
    propertyBuilder.setContentLanguage(descriptor.getContentLanguage)
    propertyBuilder.setContentDispositionType(descriptor.getContentDispositionType)
    propertyBuilder.setContentDispositionParameters(descriptor.getContentDispositionParameters)
    propertyBuilder.setContentTypeParameters(descriptor.getContentTypeParameters)

    // Add missing types
    val codeset: String = descriptor.getCharset
    if (codeset == null) {
      if ("TEXT".equalsIgnoreCase(mediaType)) {
        propertyBuilder.setCharset("us-ascii")
      }
    }
    else {
      propertyBuilder.setCharset(codeset)
    }

    val boundary: String = descriptor.getBoundary
    if (boundary != null) {
      propertyBuilder.setBoundary(boundary)
    }

    if ("text".equalsIgnoreCase(mediaType)) {
      var lines: Long = -1
      val bodyStream: CountingInputStream = new CountingInputStream(parser.getInputStream)
      try {
        bodyStream.readAll
        lines = bodyStream.getLineCount
      }
      finally {
        IOUtils.closeQuietly(bodyStream)
      }
      next = parser.next
      if (next eq EntityState.T_EPILOGUE) {
        val epilogueStream: CountingInputStream = new CountingInputStream(parser.getInputStream)
        try {
          epilogueStream.readAll
          lines += epilogueStream.getLineCount
        }
        finally {
          IOUtils.closeQuietly(epilogueStream)
        }
      }
      val lin:java.lang.Long = lines
      propertyBuilder.setTextualLineCount(lin)
    }
  }

  /**
   * Return the position in the given {@link InputStream} at which the Body of
   * the Message starts
   *
   * @param msgIn
   * @return bodyStartOctet
   * @throws IOException
   */
  private def getBodyStartOctet(msgIn: InputStream): Int = {
    val in: PushbackInputStream = new PushbackInputStream(msgIn, 3)
    var localBodyStartOctet: Int = in.available
    var i: Int = -1
    var count: Int = 0
    while ((({
      i = in.read; i
    })) != -1 && in.available > 4) {
      if (i == 0x0D) {
        val a: Int = in.read
        if (a == 0x0A) {
          val b: Int = in.read
          if (b == 0x0D) {
            val c: Int = in.read
            if (c == 0x0A) {
              localBodyStartOctet = count + 4
              return localBodyStartOctet
            }
            in.unread(c)
          }
          in.unread(b)
        }
        in.unread(a)
      }
      count += 1
    }
    return localBodyStartOctet
  }

  def getInternalDate = null

  def getMailboxId = 0

  def getUid = uid

  def setUid(p1: Long) {
    throw new UnsupportedOperationException
  }

  def setModSeq(p1: Long) {
    this.modseq = p1
  }

  def getModSeq = modseq

  def isAnswered = false

  def isDeleted = false

  def isDraft = false

  def isFlagged = false

  def isRecent = false

  def isSeen = false

  def setFlags(p1: Flags) {}

  def createFlags() = {
    val flags: Flags = new Flags
    if (isAnswered) {
      flags.add(Flags.Flag.ANSWERED)
    }
    if (isDeleted) {
      flags.add(Flags.Flag.DELETED)
    }
    if (isDraft) {
      flags.add(Flags.Flag.DRAFT)
    }
    if (isFlagged) {
      flags.add(Flags.Flag.FLAGGED)
    }
    if (isRecent) {
      flags.add(Flags.Flag.RECENT)
    }
    if (isSeen) {
      flags.add(Flags.Flag.SEEN)
    }
    flags
  }

  def getBodyContent = null

  def getMediaType = ""

  def getSubType = ""

  def getBodyOctets = 0L

  def getFullContentOctets = 0L

  def getTextualLineCount = null

  def getHeaderContent = null

  def getFullContent = null

  def compareTo(o: Message[Int]) = 0

  def getProperties = null
}
