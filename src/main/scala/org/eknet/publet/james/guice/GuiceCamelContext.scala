/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

import org.apache.camel.impl.{JndiRegistry, DefaultCamelContext}
import com.google.inject.{Key, Injector, Inject, Singleton}
import javax.annotation.{PreDestroy, PostConstruct}
import org.apache.camel.spi.{Injector => CamelInjector, _}
import javax.naming.{InitialContext, Context}
import org.apache.camel.builder.ErrorHandlerBuilder
import java.util
import org.apache.camel.{IsSingleton, TypeConverter}

/**
 * extending CamelContext to allow guice bindings to be injected.
 *
 * The original java code was taken with great thanks from `camel-guice` module
 * which is licensed under the Apache License 2.0.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 23.10.12 21:16
 */
@Singleton
class GuiceCamelContext @Inject() (injector: Injector) extends DefaultCamelContext {

  @PostConstruct
  override def start() {
    super.start()
  }

  @PreDestroy
  override def stop() {
    super.stop()
  }

  @Inject(optional = true)
  override def setRegistry(registry: Registry) {
    super.setRegistry(registry)
  }

  @Inject(optional = true)
  override def setJndiContext(jndiContext: Context) {
    super.setJndiContext(jndiContext)
  }

  @Inject(optional = true)
  override def setInjector(injector: CamelInjector) {
    super.setInjector(injector)
  }

  @Inject(optional = true)
  override def setComponentResolver(componentResolver: ComponentResolver) {
    super.setComponentResolver(componentResolver)
  }

  @Inject(optional = true)
  override def setAutoCreateComponents(autoCreateComponents: Boolean) {
    super.setAutoCreateComponents(autoCreateComponents)
  }

  @Inject(optional = true)
  override def setErrorHandlerBuilder(errorHandlerBuilder: ErrorHandlerBuilder) {
    super.setErrorHandlerBuilder(errorHandlerBuilder)
  }

  @Inject(optional = true)
  override def setInterceptStrategies(interceptStrategies: util.List[InterceptStrategy]) {
    super.setInterceptStrategies(interceptStrategies)
  }

  @Inject(optional = true)
  override def setLanguageResolver(languageResolver: LanguageResolver) {
    super.setLanguageResolver(languageResolver)
  }

  @Inject(optional = true)
  override def setLifecycleStrategies(lifecycleStrategies: util.List[LifecycleStrategy]) {
    super.setLifecycleStrategies(lifecycleStrategies)
  }

  @Inject(optional = true)
  override def setTypeConverter(typeConverter: TypeConverter) {
    super.setTypeConverter(typeConverter)
  }

  override def createInjector(): CamelInjector = new GuiceCamelInjector(injector)

  override def createRegistry() = {
    val context = createContext
    new JndiRegistry(context)
  }

  protected def createContext = {
    import collection.JavaConversions._

    def keyType(key: Key[_]) = key.getTypeLiteral.getType match {
        case c:Class[_] => Some(c)
        case _ => None
      }

    val base = classOf[Context]
    val bindings = injector.getBindings
      .filter(entry => keyType(entry._1) match {
        case Some(clazz) => base.isAssignableFrom(clazz)
        case _ => false
      })

    if (bindings.isEmpty)
      new InitialContext()
    else
      injector.getInstance(classOf[Context])
  }
}

class GuiceCamelInjector(injector: Injector) extends CamelInjector {
  def newInstance[T](clazz: Class[T]) = injector.getInstance(clazz)

  def newInstance[T](clazz: Class[T], instance: Any) = instance match {
      case s: IsSingleton if (s.isSingleton) => clazz.cast(s)
      case _ => newInstance(clazz)
    }
}
