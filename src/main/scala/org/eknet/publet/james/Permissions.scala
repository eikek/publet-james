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

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 26.10.12 23:19
 */
object Permissions {

  val mailgroup = "mailgroup"

  def removeAlias(login:String) = "james:alias:remove:"+login
  def addAlias(login:String) = "james:alias:add:"+login
  def getAlias(login:String) = "james:alias:get:"+login

  def addDomain(domain:String) = "james:domain:add:"+domain
  def removeDomain(domain:String) = "james:domain:remove:"+domain

  def getFetchmailAccount(login: String) = "james:fetchmail:account:get:"+login
  def removeFetchmailAccount(login: String) = "james:fetchmail:account:delete:"+login
  def addFetchmailAccount(login: String) = "james:fetchmail:account:add:"+login

  val getFetchmailScheduler = "james:fetchmail:scheduler:get"
  val startFetchmailScheduler = "james:fetchmail:scheduler:start"
  val stopFetchmailScheduler = "james:fetchmail:scheduler:stop"
  val setFetchmailScheduler = "james:fetchmail:scheduler:set"

  val getMappings = "james:mappings:get"
  val addMappings = "james:mappings:add"
  val removeMappings = "james:mappings:remove"

  def getServer(stype: String) = "james:server:get:"+stype
  def stopServer(stype: String) = "james:server:stop:"+stype
  def startServer(stype: String) = "james:server:start:"+stype

  def sieveUpdate(login: String) = "james:sieve:update:"+ login
  def sieveGet(login: String) = "james:sieve:get:"+ login
  val sieveManage = "james:sieve:manage"
}
