package org.eknet.publet.james.data

import java.io._
import org.apache.mailet.Mail
import java.nio.file.{FileVisitResult, Path, SimpleFileVisitor, Files}
import java.nio.file.attribute.BasicFileAttributes
import collection.mutable.ListBuffer
import com.google.common.collect.AbstractIterator
import org.apache.james.core.{MimeMessageSource, MimeMessageCopyOnWriteProxy}
import javax.mail.util.SharedFileInputStream
import org.apache.james.lifecycle.api.Disposable
import grizzled.slf4j.Logging

/**
 * Stores mails in the file system.
 *
 * On insert a unique key is created which can be used to lookup
 * the mail in the spool. Returned objects are of type
 * [[org.eknet.publet.james.data.FileSpool#MailHandle]] that combines
 * the key and the mail object.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.12.12 16:57
 */
class FileSpool(dir: File, sync:Boolean) extends Logging {

  private def visitFiles[A](f: MailFile => A): List[A] = {
    val buf = new ListBuffer[A]()
    if (dir.exists()) {
      Files.walkFileTree(dir.toPath, new SimpleFileVisitor[Path]() {
        override def visitFile(file: Path, attrs: BasicFileAttributes) = {
          if (MailFile.filter(file.toFile)) {
            buf += f(MailFile(file.toFile))
          }
          FileVisitResult.CONTINUE
        }
      })
    }
    buf.toList
  }

  def size = visitFiles(mf => 1).foldLeft(0)(_ + _)
  def clear() = visitFiles(mf => { mf.delete(); 1 }).foldLeft(0)(_ + _)

  def all: Iterable[MailHandle] = {
    import collection.JavaConversions._
    new Iterable[MailHandle]() {
      def iterator = new FileIter
    }
  }

  def get(name: String): Option[MailHandle] =  {
    Key(name).mailFile match {
      case mf if (mf.exists) => Some(MailHandle(mf.key, mf.read))
      case _ => None
    }
  }

  def put(mail: Mail): String = {
    val key = new Key(mail)
    if (!key.mailFile.exists) {
      key.mailFile.write(mail)
    }
    key.name
  }

  def remove(name: String) = {
    Key(name).mailFile match {
      case mf if (mf.exists) => mf.delete(); true
      case _ => visitFiles(mf => if (mf.key == name) {mf.delete(); true} else false).head
    }
  }

  final case class Key(name: String) {
    def this(mail: Mail) = this(mail.getName)
    //takes the last two characters of the uuid to create directory for distributing files
    //this is actually coded against the mail.name impl, needs review if new version of james
    //is used. It must always be two random characters that can be obtained from the mail name.
    private val num = MailFile.findUUID(name).map(_.take(2)).getOrElse {
      warn("No UUID found in mail name: "+ name)
      name.take(2)
    }

    val mailFile = MailFile(new File(new File(dir, num), name))
  }

  final class FileIter extends AbstractIterator[MailHandle] {
    private val files = visitFiles(identity).iterator

    def computeNext(): MailHandle = {
      if (files.hasNext) {
        val f = files.next()
        if (f.exists) f.toHandle else computeNext()
      } else {
        endOfData()
      }
    }
  }

  /**
   * Maps a mail object to two files on the file system. One contains
   * the mail object meta data and the other contains the mime message.
   * These two files are necessary, because readObject/writeObject of
   * [[org.apache.james.core.MailImpl]] do not include the mime message.
   *
   * @param dir
   * @param key
   */
  case class MailFile(dir: File, key: String) {
    private val objectFile = new File(dir, key + ".obj")
    private val messageFile = new File(dir, key + ".msg")

    def exists = objectFile.exists() && messageFile.exists()

    def read:Mail = {
      val ooin = new ObjectInputStream(new BufferedInputStream(new FileInputStream(objectFile)))
      val mail = ooin.readObject().asInstanceOf[Mail]
      ooin.close()
      mail.setMessage(new MimeMessageCopyOnWriteProxy(new MessageSource(messageFile)))
      mail
    }
    def write(mail: Mail) {
      if (!objectFile.exists()) {
        Files.createDirectories(objectFile.getParentFile.toPath)
        objectFile.createNewFile()
      }
      if (!messageFile.exists()) {
        Files.createDirectories(messageFile.getParentFile.toPath)
        messageFile.createNewFile()
      }
      val fout1 = new FileOutputStream(objectFile)
      val fout2 = new FileOutputStream(messageFile)
      val oout = new ObjectOutputStream(new BufferedOutputStream(fout1))
      oout.writeObject(mail)
      oout.flush()
      if (sync) fout1.getFD.sync()
      oout.close()
      val out = new BufferedOutputStream(fout2)
      mail.getMessage.writeTo(out)
      out.flush()
      if (sync) fout2.getFD.sync()
      out.close()
    }
    def delete() {
      Files.delete(objectFile.toPath)
      Files.delete(messageFile.toPath)
    }
    def toHandle = MailHandle(key, read)
  }
  object MailFile {
    private val UuidRegex = ".*([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}).*".r
    def findUUID(name: String) = name match {
      case UuidRegex(uuid) => Some(uuid)
      case _ => None
    }

    def filter(file: File) = file.getName.endsWith(".obj")

    val fileFilter = new FileFilter {
      def accept(pathname: File) = filter(pathname)
    }
    private def stripExtension(key: String) = key.replaceAll("\\.[a-zA-Z]*$", "")
    def apply(file: File): MailFile = MailFile(file.getParentFile, stripExtension(file.getName))
  }

  class MessageSource(file: File) extends MimeMessageSource with Disposable {
    private val in = new SharedFileInputStream(file)
    def getSourceId = file.getAbsolutePath
    def getInputStream = in.newStream(0, -1)
    override def getMessageSize = file.length()
    def dispose() {
      try { in.close() } catch {
        case e: Exception =>
      }
    }
  }

  case class MailHandle(key: String, mail: Mail)
}
