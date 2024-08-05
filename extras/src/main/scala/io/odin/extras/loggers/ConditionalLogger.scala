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

package io.odin.extras.loggers

import io.odin.{Level, Logger, LoggerMessage}
import io.odin.loggers.DefaultLogger

import cats.effect.kernel.{Async, Clock, Resource}
import cats.effect.kernel.Resource.ExitCase
import cats.effect.std.Queue
import cats.syntax.applicativeError.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.MonadError

final private[loggers] case class ConditionalLogger[F[_]: Clock](
    queue: Queue[F, LoggerMessage],
    inner: Logger[F],
    override val minLevel: Level
)(implicit F: MonadError[F, Throwable])
    extends DefaultLogger[F](minLevel) {

  def withMinimalLevel(level: Level): Logger[F] = copy(inner = inner.withMinimalLevel(level), minLevel = level)

  def submit(msg: LoggerMessage): F[Unit] = queue.tryOffer(msg).void

  private[loggers] def drain(exitCase: ExitCase): F[Unit] = {
    val level = exitCase match {
      case ExitCase.Succeeded => inner.minLevel
      case _                  => minLevel
    }

    drainAll.flatMap(msgs => inner.withMinimalLevel(level).log(msgs.toList)).voidError
  }

  private def drainAll: F[Vector[LoggerMessage]] =
    F.tailRecM(Vector.empty[LoggerMessage]) { acc =>
      queue.tryTake.map {
        case Some(value) => Left(acc :+ value)
        case None        => Right(acc)
      }
    }

}

object ConditionalLogger {

  /**
    * Create ConditionalLogger that buffers messages and sends them to the inner logger when the resource is released.
    * If evaluation of the bracket completed with an error, the `fallbackLevel` is used as a `minLevel`.
    *
    * Example:
    * {{{
    *   consoleLogger[F](minLevel = Level.Info).withErrorLevel(Level.Debug) { logger =>
    *     logger.debug("debug message") >> trickyCode
    *   }
    * }}}
    *
    * If evaluation completed with an error, the messages with `level >= Level.Debug` will be sent to an inner logger.
    * If evaluation completed successfully, the messages with `level >= Level.Info` will be sent to an inner logger.
    *
    * '''Important:''' nothing is logged until the resource is released.
    * Example:
    * {{{
    * consoleLogger[F](minLevel = Level.Info).withErrorLevel(Level.Debug) { logger =>
    *   logger.info("info log") >> Timer[F].sleep(10.seconds) >> logger.debug("debug log")
    * }
    * }}}
    *
    * The message will be logged after 10 seconds. Thus use the logger with caution.
    *
    * @param inner logger that will receive messages from the buffer
    * @param minLevelOnError min level that will be used in case of error
    * @param maxBufferSize If `maxBufferSize` is set to some value and buffer size grows to that value,
    *                      any new events might be dropped until there is a space in the buffer.
    */
  def withConditional[F[_]: Async](
      inner: Logger[F],
      minLevelOnError: Level,
      maxBufferSize: Option[Int]
  ): Resource[F, Logger[F]] = {

    val createQueue = maxBufferSize match {
      case Some(value) => Queue.bounded[F, LoggerMessage](value)
      case None        => Queue.unbounded[F, LoggerMessage]
    }

    def acquire: F[ConditionalLogger[F]] = createQueue.map(ConditionalLogger(_, inner, minLevelOnError))

    def release(logger: ConditionalLogger[F], exitCase: ExitCase): F[Unit] = logger.drain(exitCase)

    Resource.makeCase(acquire)(release).widen
  }

}
