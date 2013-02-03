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

import util.parsing.combinator.RegexParsers
import java.util.concurrent.TimeUnit
import java.util.UUID
import management.ManagementFactory
import java.net.InetAddress
import javax.mail.Flags
import java.nio.file.Path

/**
 * A maildir message name as specified in [[http://cr.yp.to/proto/maildir.html]]
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 20:40
 */
final case class MessageName(time: Long, unique: String, host: String, attributes: Map[String, String] = Map(), flags: Set[String] = Set()) {

  lazy val baseName = {
    val buf = new StringBuilder
    buf.append(time).append(".").append(unique).append(".").append(host)
    buf.toString()
  }

  lazy val fullName = {
    val buf = new StringBuilder
    buf.append(baseName)

    //a name that is accepted by james parser
    val attrs = if (attributes.isEmpty) Map("S"->"100") else attributes
    buf.append(",")
    for (kv <- attrs) {
      buf.append(kv._1).append("=").append(kv._2)
    }

    buf.append(":2,")
    for (f <- flags.toList.sorted) {
      buf.append(f)
    }
    buf.toString()
  }

  def withFlagDeleted = copy(flags = this.flags + "T")
  def withFlagSeen = copy(flags = this.flags + "S")
  def withFlagDraft = copy(flags = this.flags + "D")
  def withFlagFlagged = copy(flags = this.flags + "F")

  def withFlags(flags: Flags) = {
    val set = (for (t <- MessageName.flagMap) yield {
      if (flags.contains(t._1)) Some(t._2) else None
    }).toList.flatten.toSet
    copy(flags = set)
  }

  def withSize(size: Int) = copy(attributes = this.attributes + ("S" -> String.valueOf(size)))

  def matchFilename(exact: Boolean = false): Path => Boolean = p => {
    if (exact) p.getFileName.startsWith(fullName) else p.getFileName.startsWith(baseName)
  }
}

object MessageName {

  private lazy val vmpid = ManagementFactory.getRuntimeMXBean.getName.takeWhile(_ != '@')

  private val flagMap = Map(
    Flags.Flag.ANSWERED -> "R",
    Flags.Flag.DELETED -> "T",
    Flags.Flag.DRAFT -> "D",
    Flags.Flag.FLAGGED -> "F",
    Flags.Flag.SEEN -> "S"
  )

  def create(hostname: Option[String] = None, size: Option[Int] = None) = {
    val buf = new StringBuilder
    buf.append(TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS))
    buf.append(".")
    buf.append(UUID.randomUUID().toString.replaceAll("-", ""))
    buf.append("-")
    buf.append(vmpid)
    buf.append(".")
    buf.append(hostname.getOrElse(InetAddress.getLocalHost.getHostName))
    size filter(_ > 0)  map { sz =>
      buf.append(",S=").append(sz)
    }
    MessageName(buf.toString())
  }

  def apply(name: String): MessageName = Parser.parseAll(Parser.messageName, name).get

  private object Parser extends RegexParsers {

    def messageName = timestamp ~ "." ~ clientUnique ~ "." ~ hostname ~ attributes ~ flags ^^ {
      case (ts ~ "." ~ uniq ~ "." ~ host ~ attrMap ~ flag) => {
        new MessageName(ts, uniq, host, attrMap, flag)
      }
      case x@_ => sys.error("Wrong message name: "+ x)
    }

    private val timestamp = "[0-9]+".r ^^ (_.toLong)
    private val clientUnique = "[^.]+".r ^^ (_.toString)
    private val hostname = "[^,:]+".r  ^^ (_.toString)
    private val attr = "[A-Z]".r  ^^ (_.toString)
    private val attrValue = "[^,:]+".r ^^ (_.toString)
    private val keyValue = attr ~ "=" ~ attrValue ^^ {
      case k ~ "=" ~ v => (k ,v)
      case _ => sys.error("Wrong key-value value")
    }
    private val attributes = opt("," ~ repsep(keyValue, ",")) ^^ {
      case Some(s ~ kv) => Map[String, String]() ++ kv
      case None => Map[String, String]()
      case x@_ => sys.error("Unknown attribute repetition: " + x)
    }
    private val flags = opt(":[21],".r ~ opt("[A-Z]+".r))  ^^ {
      case Some(_ ~ Some(f)) => f.toCharArray.map(_.toString).toSet
      case x@_ => Set[String]()
    }
  }
}