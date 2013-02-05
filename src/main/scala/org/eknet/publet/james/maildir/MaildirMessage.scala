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

import lib.{MessageFile, MessageName}
import org.apache.james.mailbox.store.mail.model.AbstractMessage
import javax.mail.Flags
import java.nio.file.{Files, Path}
import collection.mutable
import java.io.PushbackInputStream
import org.bouncycastle.mail.smime.util.SharedFileInputStream
import org.apache.james.mime4j.stream.{EntityState, RecursionMode, MimeTokenStream, MimeConfig}
import org.apache.james.mime4j.message.{MaximalBodyDescriptor, DefaultBodyDescriptorBuilder}
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder
import com.google.common.io.ByteStreams
import org.apache.james.mailbox.store.streaming.{LimitingFileInputStream, CountingInputStream}
import java.util.{Objects, Date}
import annotation.tailrec

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 31.01.13 12:42
 */
class MaildirMessage(mailboxId: Int, var uid: Long, name: MessageName, file: Path) extends AbstractMessage[Int] {

  private var modseq: Long = Files.getLastModifiedTime(file).toMillis
  private val flags = new mutable.HashSet[Flags.Flag]()

  private lazy val (bodyStartByte, propertyBuilder) = MaildirMessage.parse(file)

  setFlags({
    val flags = name.getFlags
    if (file.getParent.getFileName.toString.toLowerCase == "new") {
      flags.add(Flags.Flag.RECENT)
    }
    flags
  })

  def getInternalDate = new Date(name.time)

  def getMailboxId = mailboxId

  def getMediaType = propertyBuilder.getMediaType

  def getSubType = propertyBuilder.getSubType

  def getTextualLineCount = propertyBuilder.getTextualLineCount

  def getProperties = propertyBuilder.toProperties

  def getFullContentOctets = {
    name.attributes.get("S").map(_.toLong) getOrElse {
      file.toFile.length()
    }
  }

  override def getFullContent = Files.newInputStream(file)

  def getHeaderContent = {
    val limit = bodyStartByte match {
      case l if (l < 0) => 0
      case l => l
    }
    new LimitingFileInputStream(file.toFile, limit)
  }

  def getBodyContent = {
    val in = getFullContent
    ByteStreams.skipFully(in, bodyStartByte)
    in
  }

  def getBodyStartOctet = bodyStartByte

  def isAnswered = flags.contains(Flags.Flag.ANSWERED)
  def isDeleted = flags.contains(Flags.Flag.DELETED)
  def isDraft = flags.contains(Flags.Flag.DRAFT)
  def isFlagged = flags.contains(Flags.Flag.FLAGGED)
  def isRecent = flags.contains(Flags.Flag.RECENT)
  def isSeen = flags.contains(Flags.Flag.SEEN)

  def setFlags(flags: Flags) {
    import Flags.Flag._
    List(ANSWERED, DELETED, DRAFT, FLAGGED, RECENT, SEEN) map addFlag(flags)_
  }

  private[this] def addFlag(flags: Flags)(f: Flags.Flag) {
    Option(flags).foreach(set => if (set.contains(f)) this.flags += f)
  }

  def setUid(uid: Long) {
    this.uid = uid
  }

  def getUid = uid

  def setModSeq(modSeq: Long) {
    this.modseq = modseq
  }

  def getModSeq = modseq
}

object MaildirMessage {

  /**
   * Parses a inputstream to create a MaildirMessage object.
   *
   * This code is a scalafied version of the one in James' [[org.apache.james.mailbox.maildir.mail.model.MaildirMessage]]
   * class.
   *
   * @return
   */
  private def parse(file: Path): (Int, PropertyBuilder) = {
    import org.eknet.publet.james.util.autoclose._
    val sharedIn = new SharedFileInputStream(file.toFile)
    val mimeConfig = new MimeConfig
    mimeConfig.setMaxLineLen(-1)
    val parser = new MimeTokenStream(mimeConfig, new DefaultBodyDescriptorBuilder())
    parser.setRecursionMode(RecursionMode.M_NO_RECURSE)

    val skip = Set(EntityState.T_BODY, EntityState.T_END_OF_STREAM, EntityState.T_START_MULTIPART)
    val propertyBuilder = new PropertyBuilder()
    sharedIn.exec {
      val bodyStartByte = getBodyStartOctets(new PushbackInputStream(sharedIn, 3), null, 0, 0)
      parser.parse(sharedIn.newStream(0, -1))
      var next = parser.next()
      while (!skip.contains(next)) {
        next = parser.next()
      }
      val descriptor = parser.getBodyDescriptor.asInstanceOf[MaximalBodyDescriptor]
      val (mediaType, subType) = Option(descriptor.getMediaType) map {
        mt => (mt, descriptor.getSubType) } getOrElse(("text", "plain"))

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

      val codeset = Option(descriptor.getCharset)
        .orElse { if (mediaType.toLowerCase == "text") Some("us-ascii") else None }
      propertyBuilder.setBoundary(codeset.orNull)
      Option(descriptor.getBoundary).map(b => propertyBuilder.setBoundary(b))

      if (mediaType.toLowerCase == "text") {
        val bodyStream = new CountingInputStream(parser.getInputStream)
        val lines: Long = bodyStream.exec {
          bodyStream.readAll()
          bodyStream.getLineCount
        }
        next = parser.next()
        val epiLines: Long = next match {
          case EntityState.T_EPILOGUE => {
            val epilogueStream = new CountingInputStream(parser.getInputStream)
            epilogueStream.exec {
              epilogueStream.readAll()
              epilogueStream.getLineCount
            }
          }
          case _ => 0
        }
        propertyBuilder.setTextualLineCount(lines + epiLines)
      }
      (bodyStartByte, propertyBuilder)
    }
  }

  private[this] val headerTerminate = Array[Byte](0x0D, 0x0A, 0x0D, 0x0A)

  @tailrec
  private[this] def getBodyStartOctets(inMsg: PushbackInputStream, input: Array[Byte], length: Int, count: Int): Int = {
    if (Objects.deepEquals(input, headerTerminate)) {
      count
    } else {
      for (i <- (1 to (length-1)).reverse) {
        inMsg.unread(input(i))
      }
      if (inMsg.available() <= 4) -1 else {
        val next = new Array[Byte](4)
        val len = inMsg.read(next)
        getBodyStartOctets(inMsg, next, len, count +1)
      }
    }
  }

  def from(boxId: Int, mf: MessageFile) = new MaildirMessage(boxId, mf.uid, mf.name, mf.file)
}