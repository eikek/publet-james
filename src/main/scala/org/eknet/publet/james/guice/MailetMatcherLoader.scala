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

package org.eknet.publet.james.guice

import org.apache.james.mailetcontainer.api.{MatcherLoader, MailetLoader}
import org.apache.mailet.{Matcher, Mailet, MailetConfig, MatcherConfig}
import javax.mail.MessagingException
import javax.mail
import com.google.inject.{Inject, Injector, Singleton}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 28.10.12 18:01
 */
@Singleton
class MailetMatcherLoader @Inject() (injector: Injector) extends MailetLoader with MatcherLoader {

  def standardMailetPackages = List("org.apache.james.transport.mailets", "org.apache.james.mailet.standard.mailets")
  def standardMatcherPackages = List("org.apache.james.transport.matchers", "org.apache.james.mailet.standard.matchers")

  def getMailet(config: MailetConfig): Mailet = synchronized {
    val name = resolveName(config)
    wrapException(name, load[Mailet](name, config))
  }

  def getMatcher(config: MatcherConfig):Matcher = synchronized {
    val name = resolveName(config)
    wrapException(name, load[Matcher](name, config))
  }

  private def wrapException[A](name: AnyRef, body: => A): A = {
    try { body }
    catch {
      case e: MessagingException => throw e
      case e: Exception => throw new mail.MessagingException("Unable to load: "+ name, e)
    }
  }

  private def loadMailetOrMatcher[A: Manifest](name: String, config: AnyRef): A = {
    val clazz = getClass.getClassLoader.loadClass(name)
    val inst = injector.getInstance(clazz)
    if (config.isInstanceOf[MailetConfig]) {
      inst.asInstanceOf[Mailet].init(config.asInstanceOf[MailetConfig])
    } else {
      inst.asInstanceOf[Matcher].init(config.asInstanceOf[MatcherConfig])
    }
    inst.asInstanceOf[A]
  }

  /**
   * Loads and returns the first successful mailet or matcher.
   *
   * @param names
   * @param config
   * @tparam A
   * @return
   */
  private def load[A: Manifest](names: List[String], config: AnyRef): A = {
    names match {
      case m::ms => try {
        loadMailetOrMatcher(m, config)
      } catch {
        case e: ClassNotFoundException => load(ms, config)
      }
      case Nil => throw new MessagingException("Unable to load mailet/matcher: "+ names)
    }
  }

  private def resolveName(config: AnyRef) = {
    val name = config match {
      case mc: MailetConfig => mc.getMailetName
      case mc: MatcherConfig => mc.getMatcherName
      case _ => sys.error("Invalid class usage. "+config+" must be Mailet- or MatcherConfig.")
    }

    name match {
      case x if (x.indexOf('.') < 1) => {
        (if (config.isInstanceOf[MatcherConfig]) standardMatcherPackages
        else standardMailetPackages).map(_ + "."+ x)
      }
      case x => List(x)
    }
  }

}
