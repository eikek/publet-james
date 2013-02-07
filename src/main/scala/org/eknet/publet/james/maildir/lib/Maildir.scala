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
import javax.mail.Flags

/**
 * A "maildir" folder. This is my implementation of what's known to be
 * a maildir.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 23:36
 */
class Maildir(val folder: Path, val options: Options = Options()) {

  private val folderName = folder.getFileName.toString
  private val curDir = folder.resolve("cur")
  private val newDir = folder.resolve("new")
  private val tmpDir = folder.resolve("tmp")
  val uidlist = options.uiddbProvider.newUidDb(this)

  private[lib] def visitMessageFiles(f: Path => FileVisitResult) {
    newDir.visitFiles(f)
    curDir.visitFiles(f)
  }

  /**
   * Checks whether this maildir exists.
   *
   * @return
   */
  def exists: Boolean = curDir.exists && newDir.exists && tmpDir.exists

  /**
   * Attempts to create this maildir by creating the three known subfolders
   * `cur`, `new` and `tmp` as well as the uiddb. If this maildir already
   * exists, an exception is thrown.
   *
   */
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

  /**
   * Deletes this maildir with all of its messages. If this maildir
   * is the root (INBOX) then the subfolders are not deleted.
   *
   */
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

  /**
   * The name of this maildir. If this is the root, the name
   * "INBOX" is returned. Otherwise it is the complete foldername
   * of the maildir.
   *
   */
  lazy val name = if (isRoot) "INBOX" else folderName

  /**
   * Returns `true` if this maildir is the root maildir (= INBOX).
   *
   * @return
   */
  def isRoot = folderName.charAt(0) != options.mailboxDelimiter

  /**
   * Returns the root maildir, which is the parent folder if this
   * is a sub folder. It returns `this` if `this` is already the root.
   *
   */
  lazy val rootMaildir = if (isRoot) this else {
    new Maildir(folder.getParent, options)
  }

  /**
   * Returns the parent maildir of this maildir. If this is the
   * root maildir, an exception is thrown.
   *
   */
  lazy val parentMaildir = {
    val mp = folderName.split(options.mailboxDelimiter).toList.tail
    val segs = if (mp != Nil) mp.reverse.tail.reverse else ioError("Cannot get parent maildir")
    val box = (segs: @unchecked) match {
      case a::Nil => Paths.get(a)
      case a::as => Paths.get(a, as: _*)
    }
    new Maildir(folder.resolve(box), options)
  }

  /**
   * Replaces '/' with mailbox delimiter and adds a delimiter in front of
   * the name.
   *
   * @param name
   * @return
   */
  private[this] def normalizeChildname(name: String) = {
    //also allow '/' for separating mailboxes
    val n = name.replace('/', options.mailboxDelimiter)
    if (n.charAt(0) == options.mailboxDelimiter) n else options.mailboxDelimiter+n
  }

  /**
   * Renames this maildir to a new name returning the new [[org.eknet.publet.james.maildir.lib.Maildir]].
   * The old maildir is still valid, but does not exist anymore.
   *
   * @param newName
   */
  def rename(newName: String): Maildir = {
    val targetName = if (isRoot) newName else normalizeChildname(newName)
    val target = folder.resolveSibling(targetName)
    if (target.exists) {
      ioError("Cannot rename this folder, because a folder with that name already exists")
    }
    folder.moveToLenient(target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.COPY_ATTRIBUTES)
    new Maildir(target, options)
  }

  /**
   * Returns the maildir that is a child to this.
   *
   * @param maildirpath
   * @return
   */
  def resolve(maildirpath: String): Maildir = {
    val path = normalizeChildname(maildirpath)
    if (isRoot) {
      new Maildir(folder / path, options)
    } else {
      rootMaildir.resolve(folderName + path)
    }
  }

  /**
   * Returns whether `path` is a sub mailbox of this mailbox. That is
   * either `path` is a direct or some deeper child to this maildir.
   *
   * @param path
   * @return
   */
  private def isSubMailbox(path: Path) = {
    if (path == folder) {
      false
    } else {
      val name = path.getFileName.toString
      path.startsWith(rootMaildir.folder) && (
        if (isRoot) {
          name.startsWith(String.valueOf(options.mailboxDelimiter))
        } else {
          name.startsWith(folderName)
        }
      )
    }
  }

  /**
   * Returns whether this mailbox has any child mailboxes.
   *
   * @return
   */
  def hasChildren = {
    val ds = rootMaildir.folder.list(folderName+options.mailboxDelimiter +"*")
    ds.find(isSubMailbox).isDefined
  }

  /**
   * Returns the child maildir folders of this maildir. If `includeSubfolder` is `true`,
   * the list returns all nested children as well. Otherwise only the direct subfolders
   * to this maildir are returned.
   *
   * The list is not ordered.
   *
   * @param includeSubfolder
   * @return
   */
  def listChildren(includeSubfolder: Boolean = false): Iterable[Maildir] = {
    val filter: Path => Boolean = p => p.isDirectory && p.getFileName.toString.startsWith(options.mailboxDelimiter.toString)
    rootMaildir.folder.list(filter)
      .collect(ChildFun(includeSubfolder))
      .flatten
      .toSet
      .map((path: String) => resolve(path))
  }

  private final case class ChildFun(subfolders: Boolean) extends PartialFunction[Path, List[String]] {

    private def normalize(path: Path) = {
      val filename = normalizeChildname(path.getFileName.toString)
      if (isRoot) Some(filename) else {
        if (filename.length > folderName.length && filename.startsWith(folderName)) {
          Some(filename.substring(folderName.length))
        } else {
          None
        }
      }
    }

    def isDefinedAt(path: Path) = path.startsWith(rootMaildir.folder) && normalize(path).isDefined

    def apply(path: Path) = normalize(path).map(partName => {
      val segments = partName.split(options.mailboxDelimiter).toList.drop(1)
      if (!subfolders) {
        pathFrom(segments).getName(0).toString :: Nil
      } else {
        // does: a.b.c => List(a), List(a,b), List(a,b,c)
        val sublists = for (el <- segments) yield {
          (el :: segments.takeWhile(_ != el).reverse).reverse
        }
        sublists.map(part => part.mkString(options.mailboxDelimiter.toString))
      }
    }) getOrElse(ioError("Internal error: `apply` does not match `isDefinedAt` in partial function"))
  }

  /**
   * Adds a new message into this maildir.
   *
   * The `hostname` and `size` are used to determine the message name. If not
   * specified the address of the localhost and the size of the given message
   * are used.
   *
   * If the message contains the `RECENT` flag, it is added to the `new` folder
   * otherwise to the `cur` folder.
   *
   * @param msg the message to add. This interface contains all methods needed
   *            to add the message to this maildir.
   * @param hostname optional hostname used for the message name
   * @param size optional size used for the message name
   * @return
   */
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
    tmpmsgFile.moveToLenient(target, StandardCopyOption.ATOMIC_MOVE)

    //update uid list
    val uid = uidlist.addMessage(msgName)
    MessageFile(uid, msgName, target)
  }

  /**
   * Moves the message with the given uid from this maildir to the given
   * maildir.
   *
   * @param uid
   * @param target
   * @return
   */
  def moveMessage(uid: Long, target: Maildir): MessageFile = {
    val msg = getMessage(uid)
    moveMessage(msg, target)
  }

  /**
   * "Moves" the given message file into the given target maildir. The file
   * is moved into the `cur` folder of the target mailbox.
   *
   * @param mf
   * @param target
   * @return
   */
  def moveMessage(mf: MessageFile, target: Maildir): MessageFile = {
    val targetFile = target.curDir / mf.name.fullName
    mf.file.moveToLenient(targetFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.COPY_ATTRIBUTES)
    val newUid = target.uidlist.addMessage(mf.name)
    uidlist.removeMessage(mf.uid)
    MessageFile(newUid, mf.name, targetFile)
  }

  private def findMessageFile(name: MessageName) = {
    curDir /? name.fullName orElse(newDir /? name.fullName) orElse {
      val filter = name.matchFilename(exact = false)
      curDir.findFile(filter) orElse newDir.findFile(filter)
    }
  }

  /**
   * Deletes the message with the given uid.
   *
   * If the message does not exist, an exception is thrown.
   *
   * @param uid
   * @return
   */
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

  /**
   * Returns `true` if the message with the given name exists
   * in the `cur` folder.
   *
   * @param name
   * @return
   */
  def isCurrent(name: MessageName) = {
    val file = curDir / name.fullName
    file.exists
  }

  /**
   * Returns `true` if the message with the given uid exists
   * in the `cur` folder.
   *
   * @param uid
   * @return
   */
  def isCurrent(uid: Long) = getMessage(uid).file.getParent == curDir

  /**
   * Moves the message with the given uid to the `cur` folder.
   *
   * @param uid
   * @return
   */
  def setCurrent(uid: Long): MessageFile = {
    setCurrent(getMessage(uid))
  }

  /**
   * Moves the given message to the `cur` folder.
   *
   * @param mf
   * @return
   */
  def setCurrent(mf: MessageFile): MessageFile = {
    val target = curDir / mf.name.fullName
    if (target != mf.file) {
      mf.file.moveTo(target, StandardCopyOption.ATOMIC_MOVE)
      uidlist.updateMessage(mf.uid, mf.name)
      MessageFile(mf.uid, mf.name, target)
    } else {
      mf
    }
  }

  /**
   * Updates the message with the given uid according to the given
   * flags. That can involve a rename and a move to either the
   * `new` or `cur` folder.
   *
   * @param uid
   * @param flags
   * @return
   */
  def setFlags(uid: Long, flags: Flags): MessageFile = {
    setFlags(getMessage(uid), flags)
  }

  /**
   * Updates the message with the given uid according to the given
   * flags. That can involve a rename and a move to either the
   * `new` or `cur` folder.
   *
   * @param mf
   * @param flags
   * @return
   */
  def setFlags(mf: MessageFile, flags: Flags): MessageFile = {
    if (mf.name.getFlags != flags) {
      val newName = mf.name.withFlags(flags)
      if (flags.contains(Flags.Flag.RECENT)) {
        setRecent(mf.copy(name = newName))
      } else {
        setCurrent(mf.copy(name = newName))
      }
    } else {
      mf
    }
  }

  /**
   * Returns `true` if the message with the given uid exists
   * in the `new` folder.
   *
   * @param uid
   * @return
   */
  def isRecent(uid: Long) = getMessage(uid).file.getParent == newDir

  /**
   * Returns `true`, if the message with the given name exists
   * in the `new` folder.
   *
   * @param name
   * @return
   */
  def isRecent(name: MessageName) = {
    val file = newDir / name.fullName
    file.exists
  }

  /**
   * Moves the message with the given uid to the `new` folder.
   *
   * @param uid
   * @return
   */
  def setRecent(uid: Long): MessageFile = {
    setRecent(getMessage(uid))
  }

  /**
   * Moves the given message to the `new` folder.
   *
   * @param mf
   * @return
   */
  def setRecent(mf: MessageFile): MessageFile = {
    val newName = mf.name.copy(flags = Set())
    val target = newDir / newName.fullName
    if (target != mf.file) {
      mf.file.moveTo(target, StandardCopyOption.ATOMIC_MOVE)
      uidlist.updateMessage(mf.uid, newName)
      MessageFile(mf.uid, newName, target)
    } else {
      mf
    }
  }

  /**
   * Finds a message with the given uid.
   *
   * @param uid
   * @return
   */
  def findMessage(uid: Long): Option[MessageFile] = {
    uidlist.findMessageName(uid) match {
      case None => None
      case Some(mn) => {
        findMessageFile(mn) match {
          case None => ioError("The message with uid '"+uid+"' not found in the file system.")
          case Some(f) => Some(MessageFile(uid, mn, f))
        }
      }
    }
  }

  /**
   * Returns the message with the given uid or throws an exception if not found.
   *
   * @param uid
   * @return
   */
  def getMessage(uid: Long): MessageFile = {
    findMessage(uid).getOrElse(ioError("Cannot find message for uid "+ uid))
  }

  /**
   * Returns all messages in this maildir in the given range.
   *
   * @param range
   * @return
   */
  def getMessages(range: UidRange): Map[Long, MessageFile] = {
    range match {
      case UidRange.Single(a) => {
        val msg = findMessage(a)
        msg.map(m => Map(m.uid -> m)).getOrElse(Map())
      }
      case _ => {
        val set = range match {
          case UidRange.Interval(a, b) => (a, b)
          case UidRange.From(a) => (a, Long.MaxValue)
          case UidRange.Until(b) => (Long.MinValue, b)
          case UidRange.Single(a) => (a, a)
          case UidRange.All => (Long.MinValue, Long.MaxValue)
        }
        uidlist.getMessageNames(set._1, set._2)
          .map(t => t._1 -> MessageFile(t._1, t._2, findMessageFile(t._2).getOrElse(ioError("Cannot find message uid="+t._1+" name="+t._2.fullName))))
      }
    }
  }

  def lastModified = scala.math.max(curDir.lastModifiedTime.toMillis, newDir.lastModifiedTime.toMillis)

  override def toString = getClass.getName+"[name="+name+", folder="+folder+", options="+options+"]"
}

case class Options(mailboxDelimiter: Char = '.', uiddbProvider: UidDbProvider = TextFileUidDb)