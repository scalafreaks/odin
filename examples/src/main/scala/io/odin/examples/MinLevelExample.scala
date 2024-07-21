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

import cats.effect.{IO, IOApp}
import io.odin.{Logger, _}

/**
  * Only warning message will be printed to the STDERR since this logger defines minimal level
  */
object MinLevelExample extends IOApp.Simple {
  val logger: Logger[IO] = consoleLogger[IO]().withMinimalLevel(Level.Warn)

  def run: IO[Unit] = {
    logger.info("Hello?") *> logger.warn("Hi there")
  }
}
