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

import org.apache.commons.configuration.HierarchicalConfiguration

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 28.10.12 14:34
 */
trait ConfigurationProvider {

  /**
   * Returns and possibly creates a configuration object for the given
   * class.
   *
   * @param c
   * @return
   */
  def getConfiguration(c: Class[_]): HierarchicalConfiguration

}
