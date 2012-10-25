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

import org.apache.james.protocols.lib.handler.ProtocolHandlerLoader
import org.apache.commons.configuration.Configuration
import com.google.inject.{Injector, Inject, Singleton}
import org.apache.james.protocols.api.handler.{LifecycleAwareProtocolHandler, ProtocolHandler}
import org.apache.james.protocols.lib.lifecycle.InitializingLifecycleAwareProtocolHandler

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 20.10.12 15:52
 */
@Singleton
class GuiceProtocolHandlerLoader @Inject() (injector: Injector) extends ProtocolHandlerLoader {

  def load(name: String, config: Configuration) = synchronized {
    val clazz = getClass.getClassLoader.loadClass(name)
    val ph = injector.getInstance(clazz).asInstanceOf[ProtocolHandler]
    ph match {
      case iph: InitializingLifecycleAwareProtocolHandler => iph.init(config)
      case _ =>
    }

    ph
  }

}
