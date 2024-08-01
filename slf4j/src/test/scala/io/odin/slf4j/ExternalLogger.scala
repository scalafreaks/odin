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

import io.odin.{Level, Logger}

import cats.effect.kernel.Sync
import cats.effect.std.Dispatcher
import cats.effect.unsafe.implicits.global
import cats.effect.IO

class ExternalLogger extends OdinLoggerBinder[IO] {

  implicit val F: Sync[IO]                = IO.asyncForIO
  implicit val dispatcher: Dispatcher[IO] = Dispatcher.sequential[IO].allocated.unsafeRunSync()._1

  val loggers: PartialFunction[String, Logger[IO]] = {
    case Level.Trace.toString => new BufferingLogger[IO](Level.Trace)
    case Level.Debug.toString => new BufferingLogger[IO](Level.Debug)
    case Level.Info.toString  => new BufferingLogger[IO](Level.Info)
    case Level.Warn.toString  => new BufferingLogger[IO](Level.Warn)
    case Level.Error.toString => new BufferingLogger[IO](Level.Error)
    case _ =>
      new BufferingLogger[IO](Level.Trace)
  }

}
