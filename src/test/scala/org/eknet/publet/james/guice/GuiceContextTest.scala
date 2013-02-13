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

package org.eknet.publet.james.guice

import org.scalatest.FunSuite
import com.google.inject.{Key, Stage, Guice}
import org.eknet.publet.web.guice.{PubletShutdownEvent, AppModule}
import javax.servlet.{Servlet, Filter, SessionTrackingMode, ServletContext}
import java.util.EventListener
import java.util
import org.eknet.publet.james.data.{MailDb, ConfigurationProvider}
import org.eknet.publet.james.stats.LoginStatsService
import com.google.inject.name.Names
import org.eknet.publet.james.maildir
import java.nio.file.Paths
import com.google.common.eventbus.EventBus

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 13.02.13 22:42
 */
class GuiceContextTest extends FunSuite {

  private val testDir = "target/publet-tests"

  test ("bootstrap guice injector") {
    try {
      System.setProperty("publet.dir", testDir)
      val injector = Guice.createInjector(Stage.PRODUCTION,
        new AppModule(new TestContext) // this module looks up all extensions from the classpath
      )
      injector.getInstance(classOf[ConfigurationProvider])
      injector.getInstance(classOf[MailDb])
      injector.getInstance(Key.get(classOf[LoginStatsService], Names.named("imap")))
      val bus = injector.getInstance(classOf[EventBus])
      bus.post(new PubletShutdownEvent(new TestContext))
    } finally {
      import maildir.lib._
      Paths.get("target/publet-tests").deleteTree()
    }
  }

  class TestContext extends ServletContext {
    def getContextPath = "/"
    def getContext(uripath: String) = this
    def getMajorVersion = 0
    def getMinorVersion = 0
    def getEffectiveMajorVersion = 0
    def getEffectiveMinorVersion = 0
    def getMimeType(file: String) = ""
    def getResourcePaths(path: String) = null
    def getResource(path: String) = null
    def getResourceAsStream(path: String) = null
    def getRequestDispatcher(path: String) = null
    def getNamedDispatcher(name: String) = null
    def getServlet(name: String) = null
    def getServlets = null
    def getServletNames = null
    def log(msg: String) {}
    def log(exception: Exception, msg: String) {}
    def log(message: String, throwable: Throwable) {}
    def getRealPath(path: String) = ""
    def getServerInfo = ""
    def getInitParameter(name: String) = ""
    def getInitParameterNames = null
    def setInitParameter(name: String, value: String) = false
    def getAttribute(name: String) = null
    def getAttributeNames = null
    def setAttribute(name: String, `object`: Any) {}
    def removeAttribute(name: String) {}
    def getServletContextName = ""
    def addServlet(servletName: String, className: String) = null
    def addServlet(servletName: String, servlet: Servlet) = null
    def addServlet(servletName: String, servletClass: Class[_ <: Servlet]) = null
    def createServlet[T <: Servlet](clazz: Class[T]): T = sys.error("not implemented")
    def getServletRegistration(servletName: String) = null
    def getServletRegistrations = null
    def addFilter(filterName: String, className: String) = null
    def addFilter(filterName: String, filter: Filter) = null
    def addFilter(filterName: String, filterClass: Class[_ <: Filter]) = null
    def createFilter[T <: Filter](clazz: Class[T]): T = sys.error("not implemented")
    def getFilterRegistration(filterName: String) = null
    def getFilterRegistrations = null
    def getSessionCookieConfig = null
    def setSessionTrackingModes(sessionTrackingModes: util.Set[SessionTrackingMode]) {}
    def getDefaultSessionTrackingModes = null
    def getEffectiveSessionTrackingModes = null
    def addListener(className: String) {}
    def addListener[T <: EventListener](t: T) {}
    def addListener(listenerClass: Class[_ <: EventListener]) {}
    def createListener[T <: EventListener](clazz: Class[T]) = sys.error("not implemented")
    def getJspConfigDescriptor = null
    def getClassLoader = null
    def declareRoles(roleNames: String*) {}
  }
}
