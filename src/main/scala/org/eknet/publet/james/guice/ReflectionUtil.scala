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

import java.lang.reflect.Method
import java.lang.annotation.Annotation

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.10.12 00:43
 */
trait ReflectionUtil {

  def findMethods(c: Class[_], f: Method => Boolean): List[Method] = {
    val methods = c.getMethods.filter(f).toList.distinct
    Option(c.getSuperclass).map(s => methods ::: findMethods(s, f)).getOrElse(methods)
  }

  def findAnnotatedMethods(c: Class[_], annots: Class[_ <: Annotation]*) = findMethods(c, { m =>
    annots.map(ac => m.isAnnotationPresent(ac)).foldLeft(true)(_ && _)
  }).distinct

}
