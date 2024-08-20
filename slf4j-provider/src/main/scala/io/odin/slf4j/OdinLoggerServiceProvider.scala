/*
 * Copyright 2024 ScalaFreaks
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

package io.odin.slf4j

import io.odin.Logger

import cats.effect.kernel.Sync
import cats.effect.std.Dispatcher
import org.slf4j.{ILoggerFactory, IMarkerFactory}
import org.slf4j.helpers.{BasicMDCAdapter, BasicMarkerFactory}
import org.slf4j.spi.{MDCAdapter, SLF4JServiceProvider}

abstract class OdinLoggerServiceProvider[F[_]] extends SLF4JServiceProvider {

  implicit def F: Sync[F]
  implicit def dispatcher: Dispatcher[F]

  def loggers: PartialFunction[String, Logger[F]]

  override def getRequestedApiVersion: String = "2.0"

  override def initialize(): Unit = ()

  override def getLoggerFactory: ILoggerFactory = new OdinLoggerFactory[F](loggers)

  override def getMarkerFactory: IMarkerFactory = new BasicMarkerFactory

  override def getMDCAdapter: MDCAdapter = new BasicMDCAdapter

}
