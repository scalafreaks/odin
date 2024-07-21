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

import cats.effect.kernel.Sync
import cats.effect.std.Dispatcher
import io.odin.Logger
import org.slf4j.ILoggerFactory
import org.slf4j.spi.LoggerFactoryBinder

abstract class OdinLoggerBinder[F[_]] extends LoggerFactoryBinder {

  implicit def F: Sync[F]
  implicit def dispatcher: Dispatcher[F]

  def loggers: PartialFunction[String, Logger[F]]

  private val factoryClass = classOf[OdinLoggerFactory[F]].getName

  override def getLoggerFactory: ILoggerFactory = new OdinLoggerFactory[F](loggers)

  override def getLoggerFactoryClassStr: String = factoryClass

}
