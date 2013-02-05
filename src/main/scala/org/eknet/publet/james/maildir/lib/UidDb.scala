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

package org.eknet.publet.james.maildir.lib

import java.nio.file._
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicLong
import grizzled.slf4j.Logging

/**
 * Maintains a list of uids -> message name mappings.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 11.01.13 01:03
 */
trait UidDb {

  def findMessageName(uid: Long): Option[MessageName]

  def getMessageNames(from: Long, to: Long): Map[Long, MessageName]

  def addMessage(name: MessageName): Long

  def updateMessage(uid: Long, name: MessageName)

  def removeMessage(uid: Long)

  def getUidValidity: Long

  def setUidValidity(validity: Long)

  def getNextUid: Long

  /**
   * Removes/clears all contents, such that `initialize` would
   * re-initialize this db.
   *
   */
  def clear()

  /**
   * Detects whether the uid list has not been initialized yet and
   * does so, if needed.
   *
   */
  def initialize()
}

class TextFileUidDb(maildir: Maildir, filename: String, lock: PathLock[Path], maxValue: Long = Long.MaxValue) extends UidDb with Logging {

  private val uidFile = maildir.folder / filename
  if (!uidFile.exists && maildir.exists) {
    initialize()
  }

  private val LineRegex = "^(\\d+)\\s([^\\s]+)$".r

  private lazy val header = new Supplier(() => Header.readFrom(uidFile))


  private def withFileLock[A](f: => A): A = lock.withLock(uidFile)(f)

  private def splitLine(line: String) = line.trim match {
    case LineRegex(uid, name) => Some((uid.toLong, name))
    case _ => None
  }
  private def toMessageLine(uid: Long, name: String) = uid +" "+ name +"\n"

  private def findAndMap[E](it: Iterator[String], trans: String => Option[E]): Option[E] = {
    if (it.hasNext) {
      trans(it.next()) match {
        case e @ Some(_) => e
        case _ => findAndMap(it, trans)
      }
    } else {
      None
    }
  }

  private def findByUid(comp: Long => Boolean)(line: String) = {
    splitLine(line) match {
      case Some((id, name)) if (comp(id)) => Some(MessageName(name))
      case Some((id, name)) => None
      case _ => ioError("Invalid uidlist line: '"+ line+ "'")
    }
  }

  def findMessageName(uid: Long) = withFileLock {
    findAndMap(uidFile.getLines.drop(1), findByUid(_ == uid)_)
  }

  def getMessageNames(from: Long, to: Long) = withFileLock {
    val filter = (id: Long) => id >= from && id <= to
    uidFile.getLines.collect({
      case LineRegex(uid, name) if (filter(uid.toLong)) => (uid.toLong, MessageName(name))
    }).toMap
  }

  def getNextUid = header.get.nextUid

  def addMessage(name: MessageName) = withFileLock {
    val fout = uidFile.getWriter(StandardOpenOption.WRITE, StandardOpenOption.APPEND)
    val uid = header.get.nextUid
    fout.write(toMessageLine(uid, name.fullName))
    fout.close()
    prepareNextAdd()
    uid
  }

  private def prepareNextAdd() {
    val nextUid = header.get.nextUid
    if (nextUid == maxValue) {
      //have to reorganize things
      uidFile.delete()
      initialize(header.get.uidvalidity+1)
    } else {
      header.get.copy(nextUid = nextUid+1).write(uidFile)
    }
    header.clear()
  }

  private def filterFile(f: (Long, String) => Option[String]) {
    val temp = Files.createTempFile("uidlist-new", ".lst")
    val tempOut = temp.getWriter(StandardOpenOption.WRITE, StandardOpenOption.CREATE)
    val lineCount = new AtomicLong(0)
    uidFile.getLines.toList map { line =>
      f(lineCount.getAndIncrement, line) match {
        case Some(s) => tempOut.write(s)
        case None =>
      }
    }
    tempOut.close()
    try {
      temp.moveTo(uidFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    } catch {
      case e: AtomicMoveNotSupportedException => temp.moveTo(uidFile, StandardCopyOption.REPLACE_EXISTING)
    }
    header.clear()
  }

  def getUidValidity = header.get.uidvalidity

  def setUidValidity(validity: Long) {
    header.get.copy(uidvalidity = validity).write(uidFile)
    header.clear()
  }

  def updateMessage(uid: Long, name: MessageName) {
    filterFile { (num, line) =>
      splitLine(line) match {
        case Some((id, fname)) if (id == uid) => Some(toMessageLine(id, name.fullName))
        case _ => Some(line+"\n")
      }
    }
  }

  def removeMessage(uid: Long) {
    filterFile { (num, line) =>
      splitLine(line) match {
        case Some((id, fname)) if (id == uid) => None
        case _ => Some(line+"\n")
      }
    }
  }

  def initialize() {
    initialize(1L)
  }


  def clear() {
    uidFile.deleteIfExists()
  }

  def initialize(uidvalidity: Long) {
    if (uidFile.notExists) {
      withFileLock {
        uidFile.getParent.ensureDirectories()
        val fout = uidFile.getWriter(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        val fileCount = new AtomicLong(1)
        fout.write(" " * Header.headerSpace) //preserve header space
        fout.write("\n")
        maildir.visitMessageFiles { mf =>
          if (fileCount.get() == maxValue) {
            ioError("The maildir '"+maildir.folder+"' is full. Maximum messages allowed: "+ maxValue)
          }
          fout.write(toMessageLine(fileCount.getAndIncrement, mf.getFileName.toString))
          FileVisitResult.CONTINUE
        }
        fout.close()
        Header(fileCount.get(), uidvalidity).write(uidFile)
      }
    }
  }


  private case class Header(version: Long = 1, nextUid: Long, uidvalidity: Long) {
    require(version == 1, "Unknown uid version: "+ version)

    def write(file: Path) {
      val rf = new RandomAccessFile(file.toFile, "rw")
      rf.seek(0)
      val line = "1 " + nextUid +" "+ uidvalidity
      rf.writeBytes(line + (" "* (Header.headerSpace-line.length-1)))
      rf.close()
    }
  }

  private object Header {

    val headerSpace = 80
    private val HeaderRegex = "^(\\d+)\\s+(\\d+)\\s+(\\d+)$".r

    def readFrom(file: Path) = {
      val firstLine = {
        val rf = new RandomAccessFile(file.toFile, "rw")
        rf.seek(0)
        val next = rf.readLine()
        rf.close()
        Option(next).map(_.trim)
      }
      parseLine(firstLine.getOrElse(sys.error("Uid file corrupted! No header found.")))
    }

    def parseLine(line: String) = line.trim match {
      case HeaderRegex(version, nextUid, validity) => Header(version.toLong, nextUid.toLong, validity.toLong)
      case h@_ => ioError("Uidfile corrupted. Unknown header string: "+ h)
    }

    def apply(nextUid: Long, validity: Long): Header = Header(1, nextUid, validity)
  }
}

object TextFileUidDb extends UidDbProvider {
  def newUidDb(maildir: Maildir) = new TextFileUidDb(maildir, "mymaildir-uidlist", new JvmLocker[Path])

  def newProvider(filename: String, lock: PathLock[Path], maxValue: Long) = new UidDbProvider {
    def newUidDb(maildir: Maildir) = new TextFileUidDb(maildir, filename, lock, maxValue)
  }
}