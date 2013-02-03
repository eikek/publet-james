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
import java.io.BufferedOutputStream
import scala.Some

/**
 * A "maildir" folder. This is my implementation of what's known to be
 * a maildir.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 23:36
 */
class Maildir(val folder: Path, val options: Options = Options()) {

  private val folderName = folder.getFileName.toString
  private val curDir = folder.resolveSibling("cur")
  private val newDir = folder.resolveSibling("new")
  private val tmpDir = folder.resolveSibling("tmp")
  val uidlist = options.uiddbProvider.newUidDb(this)

  private[lib] def visitMessageFiles(f: Path => FileVisitResult) {
    newDir.visitFiles(f)
    curDir.visitFiles(f)
  }

  def exists: Boolean = curDir.exists && newDir.exists && tmpDir.exists

  def create() {
    if (exists) {
      ioError("The maildir already exists")
    }
    folder.ensureDirectories()
    curDir.ensureDirectory()
    newDir.ensureDirectory()
    tmpDir.ensureDirectory()
    uidlist.initialize()
  }

  def delete() {
    if (!exists) {
      ioError("The maildir does not exist.")
    }
    if (isRoot) {
      curDir.deleteTree()
      curDir.deleteIfExists()
      newDir.deleteTree()
      newDir.deleteIfExists()
      tmpDir.deleteTree()
      tmpDir.deleteIfExists()
      uidlist.clear()
    } else {
      folder.deleteTree()
      folder.deleteIfExists()
    }
  }

  lazy val name = if (isRoot) "INBOX" else folderName

  def isRoot = folderName.charAt(0) != options.mailboxDelimiter

  lazy val rootMaildir = if (isRoot) this else {
    new Maildir(folder.getParent, options)
  }

  lazy val parentMaildir = {
    val mp = folderName.split(options.mailboxDelimiter).toList.tail
    val segs = if (!mp.isEmpty) mp.reverse.tail.reverse else ioError("Cannot get parent maildir")
    val box = segs match {
      case a::Nil => Paths.get(a)
      case a::as => Paths.get(a, as: _*)
    }
    new Maildir(folder.resolve(box), options)
  }

  private[this] def normalizeChildname(name: String) = {
    //also allow '/' for separating mailboxes
    val n = name.replace('/', options.mailboxDelimiter)
    if (n.charAt(0) == options.mailboxDelimiter) n else options.mailboxDelimiter+n
  }

  def rename(newName: String) {
    val targetName = if (isRoot) newName else normalizeChildname(newName)
    val target = folder.resolveSibling(targetName)
    if (target.exists) {
      ioError("Cannot rename this folder, because a folder with that name already exists")
    }
    folder.moveTo(target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.COPY_ATTRIBUTES)
  }

  /**
   * Returns the maildir that is a child to this.
   *
   * @param maildirpath
   * @return
   */
  def resolve(maildirpath: String) = {
    val path = normalizeChildname(maildirpath)
    new Maildir(folder / path, options)
  }

  private def isSubMailbox(path: Path) = {
    val name = path.getFileName.toString
    path.isDirectory && path.startsWith(folder) && (
      if (isRoot) {
        name.startsWith(String.valueOf(options.mailboxDelimiter))
      } else {
        name.startsWith(folderName)
      }
    )
  }

  private def isChild(path: Path) = {
    val name = path.getFileName.toString
    val lastDelimiterIdx = if (isRoot) 0 else {
      folderName.length +1
    }
    isSubMailbox(path) && name.indexOf(options.mailboxDelimiter, lastDelimiterIdx) < 0
  }


  /**
   * Returns whether this mailbox has any child mailboxes.
   *
   * @return
   */
  def hasChildren = {
    val ds = folder.list(options.mailboxDelimiter +"*")
    ds.find(isSubMailbox).isDefined
  }

  def listChildren(includeSubfolder: Boolean = false) = {
    val filter: Path => Boolean = if (includeSubfolder) isSubMailbox else isChild
    folder.list(filter).map(path => new Maildir(path, options))
  }

  def putMessage(msg: MyMessage, hostname: Option[String] = None, size: Option[Int] = None) = {
    val tmpName = MessageName.create(hostname, size.orElse(msg.getSize))
    val tmpmsgFile = tmpDir / tmpName.fullName

    // write to tmp
    val out = new BufferedOutputStream(tmpmsgFile.getOutput(StandardOpenOption.CREATE_NEW))
    msg.writeTo(out)
    out.close()

    //move to correct folder
    val msgName = tmpName.withFlags(msg.getFlags)
    val target = (if (msg.isRecent) newDir else curDir) / msgName.fullName
    tmpmsgFile.moveTo(target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.COPY_ATTRIBUTES)

    //update uid list
    val uid = uidlist.addMessage(msgName)
    MessageFile(uid, msgName, target)
  }

  private def findMessageFile(name: MessageName) = {
    curDir /? name.fullName orElse(newDir /? name.fullName) orElse {
      val filter = name.matchFilename(exact = false)
      curDir.findFile(filter) orElse newDir.findFile(filter)
    }
  }

  def deleteMessage(uid: Long) = {
    uidlist.findMessageName(uid) match {
      case None => ioError("Cannot find message for uid "+ uid)
      case Some(mn) => {
        findMessageFile(mn) match {
          case Some(f) => {
            f.delete()
            uidlist.removeMessage(uid)
            MessageFile(uid, mn, f)
          }
          case None => ioError("Unable to find file for message '"+mn+"' with uid: "+ uid)
        }
      }
    }
  }

  def getMessage(uid: Long): MessageFile = {
    uidlist.findMessageName(uid) match {
      case None => ioError("Cannot find message for uid "+ uid)
      case Some(mn) => {
        findMessageFile(mn) match {
          case None => ioError("Unable to find file for message '"+mn+"' with uid: "+ uid)
          case Some(f) => MessageFile(uid, mn, f)
        }
      }
    }
  }

  def getMessages(range: UidRange): Map[Long, MessageFile] = {
    val set = range match {
      case UidRange.Interval(a, b) => (a, b)
      case UidRange.From(a) => (a, Long.MaxValue)
      case UidRange.Until(b) => (Long.MinValue, b)
    }
    uidlist.getMessageNames(set._1, set._2)
      .map(t => t._1 -> MessageFile(t._1, t._2, findMessageFile(t._2).get))
  }

  def lastModified = scala.math.max(curDir.lastModifiedTime.toMillis, newDir.lastModifiedTime.toMillis)
}

case class Options(mailboxDelimiter: Char = '.', uiddbProvider: UidDbProvider = TextFileUidDb)