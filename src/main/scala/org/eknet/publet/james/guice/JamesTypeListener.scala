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
import com.google.inject.{Injector, TypeLiteral}
import org.apache.james.lifecycle.api.{Configurable, LogEnabled}
import org.slf4j.LoggerFactory
import javax.annotation.{PostConstruct, Resource}
import org.apache.camel.{CamelContext, CamelContextAware}
import org.apache.james.mailrepository.api.MailRepository
import java.util.concurrent.ConcurrentHashMap
import grizzled.slf4j.Logging

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.10.12 00:42
 */
class JamesTypeListener extends TypeListener with ReflectionUtil with Logging {

  private val afterPropertiesSet = "afterPropertiesSet"

//  private val map = new ConcurrentHashMap[Any, java.lang.Integer]()

  def hear[I](literal: TypeLiteral[I], encounter: TypeEncounter[I]) {
    val configProvider = encounter.getProvider(classOf[JamesConfigurationProvider])
    val injectorProvider = encounter.getProvider(classOf[Injector])
    encounter.register(new InjectionListener[I] {
      def afterInjection(injectee:I) {
        synchronized {
          injectLogger(injectee)
          val inst = injectee.asInstanceOf[AnyRef]
//          if (map.putIfAbsent(inst.getClass, 1) != null) {
//            warn("\n\n\n>>>>> duplicate init: " + inst + "\n\n\n")
//          }
          injectResourceFields(injectorProvider.get(), inst)
          injectResourceMethods(injectorProvider.get(), inst)
          injectCamelContext(injectorProvider.get(), inst)

          if (!inst.isInstanceOf[MailRepository]) {
            //todo special handling for MailRepository
            injectConfig(configProvider.get(), injectee)
            initialize(inst)
          }
        }
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
      val args = m.getParameterTypes.map(par => injector.getInstance(par))
      if (args.size == 1) {
        m.invoke(instance, args(0).asInstanceOf[AnyRef])
      } else {
        m.invoke(instance, args)
      }
    }
  }
  def injectResourceFields(injector: Injector, instance: AnyRef) {
    val fields = findAnnotatedFields(instance.getClass, classOf[Resource])
    for (f <- fields) {
      f.setAccessible(true)
      f.set(instance, injector.getInstance(f.getType))
    }
  }

  def injectCamelContext(injector: Injector, intance: AnyRef) {
    intance match {
      case ca: CamelContextAware => ca.setCamelContext(injector.getInstance(classOf[CamelContext]))
      case _ =>
    }
  }

  def initialize(instance: AnyRef) {
    val c = instance.getClass
    val all = findAnnotatedMethods(c, classOf[PostConstruct]) ::: findMethods(c, m => m.getName == afterPropertiesSet)
    all.distinct.foreach(_.invoke(instance))
  }
}
