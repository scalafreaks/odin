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

package io.odin.loggers

import java.io.PrintStream

import cats.effect.kernel.Sync
import cats.syntax.all._
import io.odin.formatter.Formatter
import io.odin.{Level, Logger, LoggerMessage}

case class ConsoleLogger[F[_]](
    formatter: Formatter,
    out: PrintStream,
    err: PrintStream,
    override val minLevel: Level
)(implicit F: Sync[F])
    extends DefaultLogger[F](minLevel) {
  private def println(out: PrintStream, msg: LoggerMessage, formatter: Formatter): F[Unit] =
    F.delay(out.println(formatter.format(msg)))

  def submit(msg: LoggerMessage): F[Unit] =
    if (msg.level < Level.Warn) {
      println(out, msg, formatter)
    } else {
      println(err, msg, formatter)
    }

  def withMinimalLevel(level: Level): Logger[F] = copy(minLevel = level)
}

object ConsoleLogger {
  def apply[F[_]: Sync](formatter: Formatter, minLevel: Level): Logger[F] =
    ConsoleLogger(formatter, scala.Console.out, scala.Console.err, minLevel)
}
