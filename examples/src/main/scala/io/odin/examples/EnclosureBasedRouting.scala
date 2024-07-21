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
import io.odin._
import io.odin.config._

/**
  * Routing based on the enclosure, would it be a package, object, class or the function.
  *
  * Mind that match is done in order of definition, therefore the most specific routes should always appear on top
  */
object EnclosureBasedRouting extends IOApp.Simple {
  val logger: Logger[IO] =
    enclosureRouting(
      "io.odin.examples.EnclosureBasedRouting.foo" -> consoleLogger[IO]().withMinimalLevel(Level.Warn),
      "io.odin.examples.EnclosureBasedRouting.bar" -> consoleLogger[IO]().withMinimalLevel(Level.Info),
      "io.odin.examples" -> consoleLogger[IO]()
    ).withNoopFallback

  def zoo: IO[Unit] = logger.debug("Debug")
  def foo: IO[Unit] = logger.info("Never shown")
  def bar: IO[Unit] = logger.warn("Warning")

  def run: IO[Unit] = {
    zoo *> foo *> bar
  }
}
