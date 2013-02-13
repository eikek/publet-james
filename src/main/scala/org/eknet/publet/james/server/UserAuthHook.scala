package org.eknet.publet.james.server

import com.google.inject.{Inject, Singleton}
import org.apache.james.smtpserver.UsersRepositoryAuthHook
import org.apache.james.user.api.UsersRepository
import org.apache.james.protocols.smtp.SMTPSession
import org.apache.james.protocols.api.handler.ExtensibleHandler
import java.util
import org.apache.james.protocols.smtp.hook.HookResultHook
import com.google.common.base.Stopwatch

/**
 * Overwriting standard [[org.apache.james.smtpserver.UsersRepositoryAuthHook]] and extend
 * it for notifying [[org.apache.james.protocols.smtp.hook.HookResultHook]]s
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 12:25
 */
//must not be a singleton
class UserAuthHook @Inject() (repo: UsersRepository) extends UsersRepositoryAuthHook with ExtensibleHandler {

  setUsersRepository(repo)

  private var hookResultListener: Iterable[HookResultHook] = Nil

  override def doAuth(session: SMTPSession, username: String, password: String) = {
    val watch = new Stopwatch().start()
    //debugging here showed that this flag was set to true?! The super.doAuth is setting it on successful auth
    session.setRelayingAllowed(false)
    val result = super.doAuth(session, username, password)
    val execTime = watch.stop().elapsedMillis()
    //set the user regardless of auth result, to be available for the hooks
    if (session.getUser == null) {
      session.setUser(username)
    }
    hookResultListener.foreach(_.onHookResult(session, result, execTime, this))
    result
  }

  def getMarkerInterfaces: util.List[Class[_]] = {
    import collection.JavaConversions._
    List(classOf[HookResultHook])
  }

  def wireExtensions(interface: Class[_], list: util.List[_]) {
    import collection.JavaConversions._
    if (interface eq classOf[HookResultHook]) {
      this.hookResultListener = list.asInstanceOf[util.List[HookResultHook]]
    }
  }
}
