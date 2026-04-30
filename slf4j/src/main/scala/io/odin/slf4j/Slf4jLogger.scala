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
      Sync[F].whenA(msg.level >= this.minLevel)((msg.level, msg.exception) match {
        case (Level.Trace, None)    => forward(msg)(logger.trace)
        case (Level.Trace, Some(t)) => forward(msg)(logger.trace(_, t))
        case (Level.Debug, None)    => forward(msg)(logger.debug)
        case (Level.Debug, Some(t)) => forward(msg)(logger.debug(_, t))
        case (Level.Info, None)     => forward(msg)(logger.info)
        case (Level.Info, Some(t))  => forward(msg)(logger.info(_, t))
        case (Level.Warn, None)     => forward(msg)(logger.warn)
        case (Level.Warn, Some(t))  => forward(msg)(logger.warn(_, t))
        case (Level.Error, None)    => forward(msg)(logger.error)
        case (Level.Error, Some(t)) => forward(msg)(logger.error(_, t))
      })
    }
  }

  private def forward(msg: LoggerMessage)(log: String => Unit): F[Unit] =
    Sync[F].suspend(syncType)(log(formatter.format(msg)))

}

object Slf4jLogger {

  def apply[F[_]: Sync](
      logger: JLogger = LoggerFactory.getLogger("OdinSlf4jLogger"),
      level: Level = Level.Info,
      formatter: Formatter = msg => msg.message.value,
      syncType: Sync.Type = Sync.Type.Blocking
  ): Logger[F] = new Slf4jLogger[F](logger, level, formatter, syncType)

}
