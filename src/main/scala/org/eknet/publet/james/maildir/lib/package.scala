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

import java.nio.file._
import attribute.BasicFileAttributes
import java.io._
import scala.Some
import javax.annotation.concurrent.NotThreadSafe
import java.nio.file.DirectoryStream.Filter

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 11.01.13 20:01
 */
package object lib {

  def ioError(msg: String, cause: Throwable = null) = Option(cause) match {
    case Some(c) => throw new IOException(msg, c)
    case None => throw new IOException(msg)
  }


  implicit def decoratePath(p: Path) = new DecoratedPath(p)
  implicit def undecoratePath(dp: DecoratedPath) = dp.path

  class DecoratedPath(val path: Path) {
    def notExists = Files.notExists(path)
    def exists = Files.exists(path)
    def createDirectory = Files.createDirectory(path)
    def createDirectories = Files.createDirectories(path)
    def createFile = Files.createFile(path)
    def delete() { Files.delete(path) }
    def deleteIfExists() = Files.deleteIfExists(path)
    def isDirectory = Files.isDirectory(path)
    def isFile = Files.isRegularFile(path)

    def / (p: Path) = path.resolve(p)
    def / (p: String) = path.resolve(p)

    def /? (p:Path) = /(p) match {
      case np if (np.exists) => Some(np)
      case _ => None
    }
    def /? (p: String) = /(p) match {
      case np if (np.exists) => Some(np)
      case _ => None
    }

    def isEmpty = if (isFile) {
      path.toUri.toURL.openStream().available() > 0
    } else if (isDirectory) {
      val ds = Files.newDirectoryStream(path)
      ds.iterator().hasNext
    } else {
      ioError("Cannot determine emptiness for "+ path)
    }

    def readFrom(in: InputStream, opts: CopyOption*) = Files.copy(in, path, opts: _*)
    def writeTo(out: OutputStream) = Files.copy(path, out)
    def copyTo(other: Path, options: CopyOption*) = Files.copy(path, other, options: _*)
    def moveTo(other: Path, options: CopyOption*) = Files.move(path, other, options: _*)

    /**
     * Same as `moveTo(Path, CopyOption*)` but it handles [[java.nio.file.AtomicMoveNotSupportedException]]
     * and retries without this options. It also handles [[java.lang.UnsupportedOperationException]] and
     * retries without any copy options.
     *
     * @param other
     * @param options
     * @return
     */
    def moveToLenient(other: Path, options: CopyOption*) = {
      try {
        moveTo(other, options: _*)
      } catch {
        case e: AtomicMoveNotSupportedException => moveTo(other, options.filter(_ != StandardCopyOption.ATOMIC_MOVE): _*)
        case e: UnsupportedOperationException => moveTo(other)
      }
    }

    def getOutput(options: OpenOption*) = Files.newOutputStream(path, options: _*)
    def getWriter(options: OpenOption*) = new BufferedWriter(new OutputStreamWriter(getOutput(options: _*)))
    def getInput(options: OpenOption*) = Files.newInputStream(path, options: _*)
    def getReader(options: OpenOption*) = new BufferedReader(new InputStreamReader(getInput(options: _*)))

    def list(glob: String): Iterable[Path] = {
      import collection.JavaConversions._
      Files.newDirectoryStream(path, glob)
    }
    def list(): Iterable[Path] = {
      import collection.JavaConversions._
      Files.newDirectoryStream(path)
    }
    def list(filter: Path => Boolean):Iterable[Path] = {
      import collection.JavaConversions._
      Files.newDirectoryStream(path, new Filter[Path] {
        def accept(entry: Path) = filter(entry)
      })
    }

    def deleteTree() = {
      Files.walkFileTree(path, new SimpleFileVisitor[Path]() {
        override def visitFile(file: Path, attrs: BasicFileAttributes) = {
          file.delete()
          FileVisitResult.CONTINUE
        }

        override def postVisitDirectory(dir: Path, exc: IOException) = {
          if (exc == null) {
            dir.delete()
            FileVisitResult.CONTINUE
          } else {
            throw exc
          }
        }
      })
    }

    def ensureFile() = {
      notExists match {
        case true => createFile
        case _ => isFile match {
          case true => path
          case _ => ioError("Path '"+path+"' exists but is not a file!")
        }
      }
    }

    def ensureDirectory() = {
      notExists match {
        case true => createDirectory
        case _ => isDirectory match {
          case true => path
          case _ => ioError("Path '"+ path +"' exists but is not a directory.")
        }
      }
    }

    def ensureDirectories() = {
      notExists match {
        case true => createDirectories
        case _ => ensureDirectory()
      }
    }

    def getLines = scala.io.Source.fromFile(path.toFile).getLines()

    def findFile(filter: Path => Boolean) = {
      var result: Option[Path] = None
      Files.walkFileTree(path, new SimpleFileVisitor[Path]() {
        override def visitFile(file: Path, attrs: BasicFileAttributes) = {
          filter(file) match {
            case true => {
              result = Some(file)
              FileVisitResult.TERMINATE
            }
            case _ => FileVisitResult.CONTINUE
          }
        }
      })
      result
    }

    def visitFiles(f: Path => FileVisitResult) {
      Files.walkFileTree(path, new SimpleFileVisitor[Path]() {
        override def visitFile(file: Path, attrs: BasicFileAttributes) = {
          f(file)
        }
      })
    }

    def lastModifiedTime = Files.getLastModifiedTime(path)
  }


  @NotThreadSafe
  class Supplier[A <: AnyRef](factory: () => A) {

    private val clean = new AnyRef
    private var ref: AnyRef = clean

    def get = {
      if (ref == clean) {
        ref = factory()
      }
      ref.asInstanceOf[A]
    }

    def clear() {
      ref = clean
    }
  }

  def pathFrom(names: List[String]): java.nio.file.Path = names match {
    case a::Nil => Paths.get(a)
    case a::as => Paths.get(a, as: _*)
    case Nil => sys.error("Name must not be empty")
  }
}
