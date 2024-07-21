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
import io.odin.{Logger => OdinLogger}
import org.slf4j.{ILoggerFactory, Logger}

class OdinLoggerFactory[F[_]: Sync: Dispatcher](loggers: PartialFunction[String, OdinLogger[F]])
    extends ILoggerFactory {
  def getLogger(name: String): Logger = {
    new OdinLoggerAdapter[F](name, loggers.applyOrElse(name, (_: String) => OdinLogger.noop))
  }
}
