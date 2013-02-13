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

package org.eknet.publet.james.data

import com.google.inject.{Provider, Inject}
import org.eknet.publet.ext.graphdb.{GraphDb, GraphDbProvider}

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 25.12.12 18:37
 */
class DbProvider @Inject() (dbfac: GraphDbProvider) extends Provider[GraphDb] {

  private val name = "jamesdb"

  def get() = dbfac.getDatabase(name)
}
