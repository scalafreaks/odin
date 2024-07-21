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
  * Only one logger will print the message, as one of them will be routed to the logger with minimal level WARN,
  * but both print only info messages
  */
object ClassBasedRouting extends IOApp.Simple {
  val logger: Logger[IO] =
    classRouting[IO](
      classOf[Foo[_]] -> consoleLogger[IO]().withMinimalLevel(Level.Warn),
      classOf[Bar[_]] -> consoleLogger[IO]().withMinimalLevel(Level.Info)
    ).withNoopFallback

  def run: IO[Unit] = {
    Foo(logger).log *> Bar(logger).log
  }
}

case class Foo[F[_]](logger: Logger[F]) {
  def log: F[Unit] = logger.info("foo")
}

case class Bar[F[_]](logger: Logger[F]) {
  def log: F[Unit] = logger.info("bar")
}
