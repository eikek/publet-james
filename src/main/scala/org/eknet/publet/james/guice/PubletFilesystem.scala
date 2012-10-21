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

import org.apache.james.filesystem.api.FileSystem
import com.google.inject.{Inject, Singleton}
import org.eknet.publet.web.Config

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 20.10.12 15:54
 */
@Singleton
class PubletFilesystem @Inject() (config: Config) extends FileSystem {

  val baseDir = config.newStaticTempDir("james")

  def getResource(url: String) = null

  def getBasedir = baseDir

  def getFile(fileURL: String) = null

}
