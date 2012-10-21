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

import com.google.inject.spi.{InjectionListener, TypeEncounter, TypeListener}
import com.google.inject.{MembersInjector, Injector, TypeLiteral}
import org.apache.james.lifecycle.api.{Configurable, LogEnabled}
import org.slf4j.LoggerFactory
import javax.annotation.{PostConstruct, Resource}
import collection.mutable

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.10.12 00:42
 */
class JamesTypeListener extends TypeListener with ReflectionUtil {

  private val afterPropertiesSet = "afterPropertiesSet"

  def hear[I](literal: TypeLiteral[I], encounter: TypeEncounter[I]) {
    val configProvider = encounter.getProvider(classOf[JamesConfigurationProvider])
    val injectorProvider = encounter.getProvider(classOf[Injector])
    encounter.register(new InjectionListener[I] {
      def afterInjection(injectee:I) {
        val c = injectee.asInstanceOf[AnyRef].getClass
        injectLogger(injectee)
        injectConfig(configProvider.get(), injectee)
        injectResourceMethods(injectorProvider.get(), injectee.asInstanceOf[AnyRef])

        findAnnotatedMethods(c, classOf[PostConstruct]).foreach(_.invoke(injectee))
        findMethods(c, m => m.getName == afterPropertiesSet).foreach(_.invoke(injectee))
      }
    })
  }


  def injectLogger(injectee: Any) {
    injectee match {
      case le: LogEnabled => le.setLog(LoggerFactory.getLogger(le.getClass))
      case _ =>
    }
  }

  def injectConfig(provider: JamesConfigurationProvider, injectee: Any) {
    injectee match {
      case c: Configurable => c.configure(provider.getConfiguration(c.getClass))
      case _ =>
    }
  }

  def injectResourceMethods(injector: Injector, instance: AnyRef) {
    val setter = findAnnotatedMethods(instance.getClass, classOf[Resource])
    for (m <- setter) {
      val args = m.getParameterTypes.map(par => par.cast(injector.getInstance(par)))
      if (args.size == 1) {
        m.invoke(instance, args(0).asInstanceOf[AnyRef])
      } else {
        m.invoke(instance, args)
      }
    }
  }
}
