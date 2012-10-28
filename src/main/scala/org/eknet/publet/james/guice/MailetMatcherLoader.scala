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

  def getMailet(config: MailetConfig): Mailet = synchronized {
    val name = resolveName(config)
    loadMailetOrMatcher(name, config)
  }

  def standardMailetPackage = "org.apache.james.transport.mailets"

  def getMatcher(config: MatcherConfig):Matcher = synchronized {
    val name = resolveName(config)
    loadMailetOrMatcher(name, config)
  }

  def standardMatcherPackage = "org.apache.james.transport.matchers"

  private def loadMailetOrMatcher[A: Manifest](name: String, config: AnyRef): A = {
    try {
      val clazz = getClass.getClassLoader.loadClass(name)
      val inst = injector.getInstance(clazz)
      if (config.isInstanceOf[MailetConfig]) {
        inst.asInstanceOf[Mailet].init(config.asInstanceOf[MailetConfig])
      } else {
        inst.asInstanceOf[Matcher].init(config.asInstanceOf[MatcherConfig])
      }
      inst.asInstanceOf[A]
    }
    catch {
      case e: MessagingException => throw e
      case e: Exception => throw new mail.MessagingException("Unable to load: "+ name, e)
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
        (if (config.isInstanceOf[MatcherConfig]) standardMatcherPackage
        else standardMailetPackage) + "."+ x
      }
      case x => x
    }
  }

}
