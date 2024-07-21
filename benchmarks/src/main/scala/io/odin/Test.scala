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

package io.odin

import java.nio.file.{Files, Paths}
import java.util.UUID

import cats.effect.{IO, IOApp}
import cats.syntax.all._
import io.odin.syntax._

object Test extends IOApp.Simple {

  val fileName: String = Files.createTempFile(UUID.randomUUID().toString, "").toAbsolutePath.toString

  val message: String = "msg"

  def run: IO[Unit] =
    fileLogger[IO](fileName)
      .withAsync(maxBufferSize = Some(1000000))
      .use { logger =>
        val io = (1 to 1000).toList.traverse(_ => logger.info(message))
        io.foreverM
      }
      .guarantee(IO.delay(Files.delete(Paths.get(fileName))))
}
