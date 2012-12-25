/*
 * Copyright 2012 Eike Kettner
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

package org.eknet.publet.james.data

import org.apache.james.filesystem.api.FileSystem
import com.google.inject.{Inject, Singleton}
import org.eknet.publet.web.Config
import java.io.{InputStream, FileInputStream, FileNotFoundException, File}
import org.eknet.publet.vfs.Path

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 20.10.12 15:54
 */
@Singleton
class PubletFilesystemImpl @Inject() (config: Config) extends PubletFilesystem {

  private val baseDir = config.workDir("james")

  import FileSystem._

  private val fileProtocols = List(
    FILE_PROTOCOL_AND_CONF,
    FILE_PROTOCOL_AND_VAR,
    FILE_PROTOCOL_ABSOLUTE,
    FILE_PROTOCOL
  )

  private val subdirMap = Map(
    FILE_PROTOCOL_AND_CONF -> config.configFile("test").getParentFile,
    FILE_PROTOCOL_AND_VAR -> new File(baseDir, "var"),
    FILE_PROTOCOL -> baseDir
  )

  def getBasedir = baseDir

  def getResource(url: String) = findResource(url).getOrElse(throw new FileNotFoundException("Resource not found: "+ url))

  def findResource(url: String): Option[InputStream] = resolveFile(url, f => f.exists())
    .map(f => new FileInputStream(f)).orElse {
      if (url.startsWith(CLASSPATH_PROTOCOL)) {
        Option(getClass.getResource(url.substring(CLASSPATH_PROTOCOL.length))).map(_.openStream())
      } else {
        None
      }
    }

  def getFile(fileURL: String) = resolveFile(fileURL, f => true)
    .getOrElse(throw new FileNotFoundException("File not found: "+ fileURL))

  def resolveFile(fileUrl: String, filter: File => Boolean): Option[File] = {
    val resolve = resolveFileUrl(fileUrl)_
    def findPair(protocols: List[String]): Option[File] = protocols match {
      case a::as => resolve(a) match {
        case Some(x) if (filter(x)) => Some(x)
        case _ => findPair(as)
      }
      case Nil => None
    }

    findPair(fileProtocols)
  }

  private def resolveFileUrl(url: String)(prefix: String) = {
    def mapFile(protocol: String, name: String): File = {
      val path = Path(name).segments.mkString(File.separator)
      subdirMap.get(protocol) match {
        case Some(subdir) => new File(subdir, path)
        case None => new File(path)
      }
    }

    if (url.startsWith(prefix)) {
      Some(mapFile(prefix, url.substring(prefix.length)))
    } else {
      None
    }
  }

}
