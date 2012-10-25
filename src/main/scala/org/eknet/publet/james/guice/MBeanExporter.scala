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
import com.google.inject.TypeLiteral
import com.google.inject.matcher.{Matchers, AbstractMatcher}
import javax.management.{ObjectName, DynamicMBean}
import java.lang.management.ManagementFactory
import grizzled.slf4j.Logging

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.10.12 23:51
 */
class MBeanExporter extends TypeListener with Logging {

  val server = ManagementFactory.getPlatformMBeanServer

  def hear[I](`type`: TypeLiteral[I], encounter: TypeEncounter[I]) {
    encounter.register(new InjectionListener[I] {
      def afterInjection(injectee: I) {
        try {
          server.registerMBean(injectee, createName(injectee.asInstanceOf[AnyRef]))
        } catch {
          case e: Exception => error("Error registering mbean: "+ injectee, e)
        }
      }
    })
  }

  private def createName(inst: AnyRef): ObjectName = {
    val pkg = inst.getClass.getName match {
      case n if (n.lastIndexOf('.') != -1) => n.substring(0, n.lastIndexOf('.'))
      case x => x
    }
    ObjectName.getInstance(pkg, "type", inst.getClass.getSimpleName)
  }
}

object MBeanMatcher extends AbstractMatcher[TypeLiteral[_]] {
  private val delegate = Matchers.subclassesOf(classOf[DynamicMBean])
  def matches(t: TypeLiteral[_]) = delegate.matches(t.getRawType)
}
