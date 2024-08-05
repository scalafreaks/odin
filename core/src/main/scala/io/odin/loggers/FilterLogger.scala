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

import cats.effect.kernel.Clock
import cats.Monad

/**
  * Filter each `LoggerMessage` using given predicate before passing it to the next logger
  */
final private[loggers] class FilterLogger[F[_]: Clock](fn: LoggerMessage => Boolean, inner: Logger[F])(
    implicit F: Monad[F]
) extends DefaultLogger[F](inner.minLevel) {

  def withMinimalLevel(level: Level): Logger[F] = new FilterLogger(fn, inner.withMinimalLevel(level))

  def submit(msg: LoggerMessage): F[Unit] =
    F.whenA(fn(msg))(inner.log(msg))

  override def submit(msgs: List[LoggerMessage]): F[Unit] =
    inner.log(msgs.filter(fn))

}

object FilterLogger {

  def withFilter[F[_]: Clock: Monad](fn: LoggerMessage => Boolean, inner: Logger[F]): Logger[F] =
    new FilterLogger(fn, inner)

}
