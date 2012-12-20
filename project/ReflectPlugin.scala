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

import sbt._
// from here: https://github.com/ritschwumm/xsbt-reflect
// mods:
//  - add timestamp to Reflect object

/**
usage
	<code>
	seq(ReflectPlugin.allSettings:_*)
	sourceGenerators in Compile <+= reflect map identity
	</code>
code
	println("project version=" + Reflect.version)
  */
object ReflectPlugin extends Plugin {
  val reflect			= TaskKey[Seq[File]]("reflect")
  val reflectPackage	= SettingKey[String]("reflect-package")
  val reflectClass	= SettingKey[String]("reflect-class")

  lazy val allSettings	= Seq(
    reflectPackage	:= "",
    reflectClass	:= "Reflect",
    reflect			<<= (Keys.sourceManaged, Keys.name, Keys.version, reflectPackage, reflectClass) map {
      (sourceManaged:File, name:String, version:String, reflectPackage:String, reflectClass:String)	=>
        val	file	= sourceManaged / "reflect" / "Reflect.scala"
        val code	=
          (
            if (reflectPackage.nonEmpty)	"package " + reflectPackage + "\n"
            else							""
            ) +
            "object " + reflectClass + " {\n" +
            "\tval name\t= \"" + name + "\"\n" +
            "\tval version\t= \"" + version + "\"\n" +
            "\tval timestamp = " + System.currentTimeMillis +"L\n"+
            "}\n"
        IO write (file, code)
        Seq(file)
    }
  )
}