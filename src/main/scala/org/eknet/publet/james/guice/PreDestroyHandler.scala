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

import com.google.common.eventbus.Subscribe
import org.eknet.publet.web.guice.PubletShutdownEvent
import com.google.inject._
import javax.annotation.PreDestroy
import grizzled.slf4j.Logging
import org.eknet.publet.web.guice.PubletShutdownEvent

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.10.12 19:21
 */
@Singleton
class PreDestroyHandler @Inject() (injector: Injector) extends ReflectionUtil with Logging {

  @Subscribe
  def executePreDestroy(ev: PubletShutdownEvent) {
    import collection.JavaConversions._

    injector.getAllBindings
      .withFilter(mapping => JamesMatcher.matches(mapping._1.getTypeLiteral))
      .foreach( predestroy )
  }

  private def predestroy(mapping: (Key[_], Binding[_]) ) {
    val clazz = mapping._1.getTypeLiteral.getRawType
    val inst = mapping._2.getProvider.get()
    findAnnotatedMethods(clazz, classOf[PreDestroy]) foreach (m => {
      info("Invoking pre-destroy hook on " +inst)
      m.invoke(inst)
    })
  }
}
