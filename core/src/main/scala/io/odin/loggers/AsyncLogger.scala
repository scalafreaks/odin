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

import io.odin.{Level, Logger, LoggerMessage}

import cats.effect.kernel.{Async, Resource}
import cats.effect.std.{Dispatcher, Queue}
import cats.syntax.all.*

/**
  * AsyncLogger spawns non-cancellable `cats.effect.Fiber` with actual log action encapsulated there.
  *
  * Use `AsyncLogger.withAsync` to instantiate it safely
  */
private[loggers] final class AsyncLogger[F[_]](
    buffer: Queue[F, (Logger[F], LoggerMessage)],
    inner: Logger[F]
)(
    implicit F: Async[F]
) extends DefaultLogger[F](inner.minLevel) {

  def withMinimalLevel(level: Level): Logger[F] = new AsyncLogger(buffer, inner.withMinimalLevel(level))

  def submit(msg: LoggerMessage): F[Unit] = buffer.offer(inner -> msg)

  def blockingDrain: F[Unit] =
    // Forbid cancellation after taking some elements from the queue
    F.uncancelable { poll => poll(buffer.take).flatMap(head => drain(Some(head))) }

  def drain(head: Option[(Logger[F], LoggerMessage)]): F[Unit] =
    buffer
      .tryTakeN(None)
      .map(tail => head.fold(tail)(_ :: tail))
      .flatMap {
        case (headLogger, headMsg) :: Nil => headLogger.log(headMsg)
        case unbuffered =>
          val unbufferedGrouped = unbuffered.groupMap { case (logger, _) => logger } { case (_, msg) => msg }
          unbufferedGrouped.toList.traverse_ { case (logger, msgs) => logger.log(msgs) }
      }
      .voidError

}

object AsyncLogger {

  /**
    * Create async logger and start internal loop of sending events down the chain from the buffer once
    * `Resource` is used
    * @param inner logger that will receive messages from the buffer
    * @param maxBufferSize If `maxBufferSize` is set to some value and buffer size grows to that value,
    *                      any new events might be dropped until there is a space in the buffer.
    */
  def withAsync[F[_]](inner: Logger[F], maxBufferSize: Option[Int])(implicit F: Async[F]): Resource[F, Logger[F]] = {

    def queue: F[Queue[F, (Logger[F], LoggerMessage)]] = maxBufferSize match {
      case Some(max) => Queue.dropping[F, (Logger[F], LoggerMessage)](max)
      case None      => Queue.unbounded[F, (Logger[F], LoggerMessage)]
    }

    // Run internal loop of consuming events from the queue and push them down the chain
    def backgroundConsumer(logger: AsyncLogger[F]): Resource[F, Unit] =
      F.background(logger.blockingDrain.foreverM).onFinalize(logger.drain(None)).void

    for {
      buffer <- Resource.eval(queue)
      logger  = new AsyncLogger(buffer, inner)
      _      <- backgroundConsumer(logger)
    } yield logger
  }

  /**
    * Create async logger and start internal loop of sending events down the chain from the buffer right away
    * @param maxBufferSize If `maxBufferSize` is set to some value and buffer size grows to that value,
    *                      any new events will be dropped until there is a space in the buffer.
    */
  def withAsyncUnsafe[F[_]](inner: Logger[F], maxBufferSize: Option[Int])(
      implicit F: Async[F],
      dispatcher: Dispatcher[F]
  ): Logger[F] = dispatcher.unsafeRunSync(withAsync(inner, maxBufferSize).allocated)._1

}
