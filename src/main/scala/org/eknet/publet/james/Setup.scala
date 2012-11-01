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

package org.eknet.publet.james

import com.google.inject.{Inject, Singleton}
import org.eknet.publet.web.guice.PubletStartedEvent
import com.google.common.eventbus.Subscribe
import org.eknet.publet.Publet
import org.eknet.publet.vfs.util.{MapContainer, ClasspathContainer}
import org.eknet.publet.vfs.Path
import org.eknet.publet.web.scripts.WebScriptResource
import org.eknet.publet.james.ui.{GetDefaultDomain, RemoveDomain, AddDomain, GetDomains}
import org.eknet.publet.web.asset.{Group, AssetCollection, AssetManager}
import org.eknet.publet.web.template.DefaultLayout

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 01.11.12 12:10
 */
@Singleton
class Setup @Inject() (publet: Publet, assetMgr: AssetManager) {

  @Subscribe
  def mountResources(event: PubletStartedEvent) {
    val container = new ClasspathContainer(base = "/org/eknet/publet/james/ui/includes")
    publet.mountManager.mount(Path("/publet/james"), container)

    import org.eknet.publet.vfs.ResourceName._
    val scripts = new MapContainer
    scripts.addResource(new WebScriptResource("getdomains.json".rn, new GetDomains))
    scripts.addResource(new WebScriptResource("adddomain.json".rn, new AddDomain))
    scripts.addResource(new WebScriptResource("removedomain.json".rn, new RemoveDomain))
    scripts.addResource(new WebScriptResource("getdefaultdomain.json".rn, new GetDefaultDomain))
    publet.mountManager.mount(Path("/publet/james/action"), scripts)
  }

  @Subscribe
  def setupAssets(event: PubletStartedEvent) {

    assetMgr setup (Groups.jamesGroup, Groups.jamesManager)
    assetMgr setup (Group("default").use(Groups.jamesManager.name))
  }

  object Groups extends AssetCollection {

    override def classPathBase = "/org/eknet/publet/james/ui/includes"

    val jamesGroup = Group("publet.james")
      .add(resource("css/james.css"))
      .add(resource("js/managedomains.js"))
      .require(DefaultLayout.Assets.jquery.name)

    val jamesManager = Group("publet.james.manager")
      .forPath("/publet/james/**")
      .require(jamesGroup.name)
  }
}
