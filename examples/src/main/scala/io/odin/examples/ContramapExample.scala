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

import cats.effect.{IO, IOApp}

/**
  * Modify logger message before it's written
  */
object ContramapExample extends IOApp.Simple {

  /**
    * This logger always appends " World" string to each message
    */
  def logger: Logger[IO] = consoleLogger[IO]().contramap(msg => msg.copy(message = msg.message.map(_ + " World")))

  def run: IO[Unit] = logger.info("Hello")

}
