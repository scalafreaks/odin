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

import io.odin.{Level, Logger, LoggerMessage}
import io.odin.formatter.Formatter
import io.odin.loggers.DefaultLogger

import cats.effect.kernel.Sync
import cats.implicits.*
import org.slf4j.{Logger as JLogger, LoggerFactory}

private[slf4j] final class Slf4jLogger[F[_]: Sync](
    logger: JLogger,
    level: Level,
    formatter: Formatter,
    syncType: Sync.Type
) extends DefaultLogger[F](level) {

  def withMinimalLevel(level: Level): Logger[F] = new Slf4jLogger(logger, level, formatter, syncType)

  def submit(msg: LoggerMessage): F[Unit] = {
    Sync[F].uncancelable { _ =>
      Sync[F].whenA(msg.level >= this.minLevel)(msg.level match {
        case Level.Trace => Sync[F].suspend(syncType)(logger.trace(formatter.format(msg)))
        case Level.Debug => Sync[F].suspend(syncType)(logger.debug(formatter.format(msg)))
        case Level.Info  => Sync[F].suspend(syncType)(logger.info(formatter.format(msg)))
        case Level.Warn  => Sync[F].suspend(syncType)(logger.warn(formatter.format(msg)))
        case Level.Error => Sync[F].suspend(syncType)(logger.error(formatter.format(msg)))
      })
    }
  }

}

object Slf4jLogger {

  def apply[F[_]: Sync](
      logger: JLogger = LoggerFactory.getLogger("OdinSlf4jLogger"),
      level: Level = Level.Info,
      formatter: Formatter = Formatter.default,
      syncType: Sync.Type = Sync.Type.Blocking
  ): Logger[F] = new Slf4jLogger[F](logger, level, formatter, syncType)

}
