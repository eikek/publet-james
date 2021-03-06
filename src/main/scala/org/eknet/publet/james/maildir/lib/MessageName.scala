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
import javax.mail
import com.google.common.collect.{HashBiMap, Maps}
import collection.mutable
import grizzled.slf4j.Logging

/**
 * A maildir message name as specified in [[http://cr.yp.to/proto/maildir.html]]
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 20:40
 */
final case class MessageName(time: Long,
                             unique: String,
                             host: String,
                             attributes: Map[String, String] = Map(),
                             flags: List[String] = Nil,
                             flagPrefix: String = "") {

  private class ListHelp[A](list: List[A]) {
    def <<+ (el: A) = {
      if (!list.contains(el)) {
        list ::: List(el)
      } else {
        list
      }
    }
  }
  private implicit def toListHelp(list:List[String])= new ListHelp(list)

  lazy val baseName = {
    val buf = new StringBuilder
    buf.append(time).append(".").append(unique).append(".").append(host)
    buf.toString()
  }

  lazy val fullName = {
    val buf = new StringBuilder
    buf.append(baseName)

    //a name that is accepted by james parser
//    val attrs = if (attributes.isEmpty) Map("S"->"100") else attributes
    for (kv <- attributes) {
      buf.append(",")
      buf.append(kv._1).append("=").append(kv._2)
    }

    if (!flagPrefix.isEmpty) {
      buf.append(flagPrefix)
    }
    else if (!flags.isEmpty) {
      buf.append(":2,")
    }
    for (f <- flags.toList.sorted) {
      buf.append(f)
    }
    buf.toString()
  }

  def withFlagDeleted = copy(flags = this.flags <<+ "T")
  def withFlagSeen = copy(flags = this.flags <<+ "S")
  def withFlagDraft = copy(flags = this.flags <<+ "D")
  def withFlagFlagged = copy(flags = this.flags <<+ "F")
  def withFlagAnswered = copy(flags = this.flags <<+ "R")

  def withFlags(flags: Flags) = {
    import collection.JavaConversions._
    val set = (for (t <- MessageName.flagBiMap) yield {
      if (flags.contains(t._1)) Some(t._2) else None
    }).flatten.toSet.toList
    // "Flags must be stored in ASCII order: e.g., '2,FRS'": http://cr.yp.to/proto/maildir.html
    copy(flags = set.sorted)
  }

  /**
   * Creates a new message name from this one as usually seen in the
   * `new` folder for recent messages. This name has no flags set
   *
   * So it will then look like either one of these:
   * {{{
   *   1355543030.15049_0.foo.org
   *   1355543030.15049_0.foo.org:2,
   * }}}
   *
   * @return
   */
  def toRecent = copy(flags = Nil)

  def getFlags: Flags = {
    val flags = new mail.Flags()
    for (sf <- this.flags) {
      flags.add(MessageName.flagBiMap.inverse().get(sf))
    }
    flags
  }

  def withSize(size: Int) = copy(attributes = this.attributes + ("S" -> String.valueOf(size)))

  def matchFilename(exact: Boolean = false): Path => Boolean = p => {
    if (exact) p.getFileName.startsWith(fullName) else p.getFileName.startsWith(baseName)
  }
}

object MessageName extends Logging {
  private lazy val vmpid = ManagementFactory.getRuntimeMXBean.getName.takeWhile(_ != '@')

  private val flagBiMap = {
    val map: HashBiMap[Flags.Flag, String] = HashBiMap.create()
    map.put(Flags.Flag.DRAFT, "D")
    map.put(Flags.Flag.FLAGGED, "F")
    map.put(Flags.Flag.ANSWERED, "R")
    map.put(Flags.Flag.SEEN, "S")
    map.put(Flags.Flag.DELETED , "T")
    map
  }

  def create(hostname: Option[String] = None, size: Option[Int] = None) = {
    val prefix = ":2,"
    MessageName(
      TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS),
      UUID.randomUUID().toString.replaceAll("-", "") +"-"+ vmpid,
      hostname.getOrElse(InetAddress.getLocalHost.getHostName),
      size.filter(_ > 0).map(sz => Map("S"->sz.toString)).getOrElse(Map()),
      Nil,
      size.filter(_ > 0).map(s=>prefix).getOrElse("")
    )
  }

  def apply(name: String): MessageName = {
    try {
      Parser.parseAll(Parser.messageName, name).get
    } catch {
      case e: RuntimeException => throw new RuntimeException("Parsing message name failed: "+ name, e)
    }
  }

  def tryParse(name: String) = {
    try {
      Some(Parser.parseAll(Parser.messageName, name).get)
    } catch {
      case e: RuntimeException => {
        warn("Invalid message name encountered: "+ name)
        None
      }
    }
  }

  def parseFunction = new PartialFunction[String, MessageName] {
    private val names = mutable.Map[String, MessageName]()

    def apply(x: String) = {
      names.remove(x) getOrElse MessageName(x)
    }

    def isDefinedAt(x: String) = {
      tryParse(x).map(mn => names.put(x, mn)).isDefined
    }
  }

  private object Parser extends RegexParsers {

    def messageName = timestamp ~ "." ~ clientUnique ~ "." ~ hostname ~ attributes ~ flags ^^ {
      case (ts ~ "." ~ uniq ~ "." ~ host ~ attrMap ~ flag) => {
        new MessageName(ts, uniq, host, attrMap.toMap, flag._2, flag._1)
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
    private def newMap = new mutable.LinkedHashMap[String, String]()

    private val attributes = opt("," ~ repsep(keyValue, ",")) ^^ {
      case Some(s ~ kv) => newMap ++ kv
      case None => Map[String, String]()
      case x@_ => sys.error("Unknown attribute repetition: " + x)
    }
    private val flags = opt(":[21],".r ~ opt("[a-zA-Z]+".r))  ^^ {
      case Some(prefix ~ Some(f)) => (prefix, f.toCharArray.map(_.toString).toList)
      case Some(prefix ~ _) => (prefix, Nil)
      case x@_ => ("", Nil)
    }
  }
}