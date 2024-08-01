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

package io.odin.examples

import io.odin.*
import io.odin.syntax.*

import cats.effect.{IO, IOApp, Resource}

/**
  * Async logger runs the internal loop to drain the buffer that accumulates log events.
  *
  * To safely allocate, release and drain this queue, async logger is wrapped in `Resource`
  */
object AsyncHelloWorld extends IOApp.Simple {

  val loggerResource: Resource[IO, Logger[IO]] = consoleLogger[IO]().withAsync()

  def run: IO[Unit] = loggerResource.use(logger => logger.info("Hello world"))

}
