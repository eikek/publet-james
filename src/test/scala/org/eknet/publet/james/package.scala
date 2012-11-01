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

package org.eknet.publet

import _root_.com.google.common.eventbus.EventBus
import _root_.com.tinkerpop.blueprints.impls.orient.OrientGraph
import org.eknet.publet.ext.orient.{BlueprintGraph, DefaultGraphDbProvider}
import org.eknet.publet.web.Config
import org.eknet.scue.OrientDbFactory

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 01.11.12 11:16
 */
package object james {

  class TestGraphDbProvider extends DefaultGraphDbProvider(new Config("", new EventBus())) {
    override def toOrientUri(dbname: String) = OrientDbFactory.dbUrl(dbname)

    def getNext = new OrientGraph(OrientDbFactory.nextDb) with BlueprintGraph
  }

}
