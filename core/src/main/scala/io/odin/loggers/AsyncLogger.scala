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

import scala.concurrent.duration.*

import io.odin.{Level, Logger, LoggerMessage}

import cats.effect.kernel.{Async, Clock, Ref, Resource}
import cats.effect.std.Dispatcher
import cats.syntax.all.*
import cats.MonadThrow

/**
  * AsyncLogger spawns non-cancellable `cats.effect.Fiber` with actual log action encapsulated there.
  *
  * Use `AsyncLogger.withAsync` to instantiate it safely
  */
private[loggers] final class AsyncLogger[F[_]: Clock](
    buffers: Ref[F, Map[Logger[F], Vector[LoggerMessage]]],
    maxBufferSize: Int,
    inner: Logger[F]
)(
    implicit F: MonadThrow[F]
) extends DefaultLogger[F](inner.minLevel) {

  def withMinimalLevel(level: Level): Logger[F] = new AsyncLogger(buffers, maxBufferSize, inner.withMinimalLevel(level))

  def submit(msg: LoggerMessage): F[Unit] = buffers.update { buffers =>
    buffers.updatedWith(inner) {
      case Some(msgs) if msgs.length < maxBufferSize => Some(msgs :+ msg)
      case None                                      => Some(Vector(msg))
      case other                                     => other // Exhausted buffer, messages are discarded
    }
  }

  def drain: F[Unit] = buffers.getAndSet(Map.empty).flatMap { buffers =>
    buffers.toList.traverse_ { case (logger, msgs) => logger.log(msgs.toList).voidError }
  }

}

object AsyncLogger {

  /**
    * Create async logger and start internal loop of sending events down the chain from the buffer once
    * `Resource` is used.
    *
    * @param inner logger that will receive messages from the buffer
    * @param timeWindow pause between buffer flushing
    * @param maxBufferSize If `maxBufferSize` is set to some value and buffer size grows to that value,
    *                      any new events might be dropped until there is a space in the buffer.
    */
  def withAsync[F[_]](
      inner: Logger[F],
      timeWindow: FiniteDuration,
      maxBufferSize: Option[Int]
  )(
      implicit F: Async[F]
  ): Resource[F, Logger[F]] = {

    // Run internal loop of consuming events from the queue and push them down the chain
    def backgroundConsumer(logger: AsyncLogger[F]): Resource[F, Unit] = {

      def drainLoop: F[Unit] = F.delayBy(logger.drain, timeWindow).foreverM

      F.background(drainLoop).onFinalize(logger.drain).void
    }

    for {
      buffers <- Resource.eval(Ref[F].of(Map.empty[Logger[F], Vector[LoggerMessage]]))
      logger   = new AsyncLogger(buffers, maxBufferSize.getOrElse(Int.MaxValue), inner)
      _       <- backgroundConsumer(logger)
    } yield logger
  }

  /**
    * Create async logger and start internal loop of sending events down the chain from the buffer right away
    * @param maxBufferSize If `maxBufferSize` is set to some value and buffer size grows to that value,
    *                      any new events will be dropped until there is a space in the buffer.
    */
  def withAsyncUnsafe[F[_]](
      inner: Logger[F],
      timeWindow: FiniteDuration,
      maxBufferSize: Option[Int]
  )(
      implicit F: Async[F],
      dispatcher: Dispatcher[F]
  ): Logger[F] = dispatcher.unsafeRunSync(withAsync(inner, timeWindow, maxBufferSize).allocated)._1

}
