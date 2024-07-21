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

import cats.Monad
import cats.effect.kernel.Clock
import io.odin.{Level, Logger, LoggerMessage}

/**
  * Apply given function to each `LoggerMessage` before passing it to the next logger
  */
case class ContramapLogger[F[_]: Clock: Monad](f: LoggerMessage => LoggerMessage, inner: Logger[F])
    extends DefaultLogger[F](inner.minLevel) {
  def submit(msg: LoggerMessage): F[Unit] = inner.log(f(msg))

  override def submit(msgs: List[LoggerMessage]): F[Unit] = inner.log(msgs.map(f))

  def withMinimalLevel(level: Level): Logger[F] = copy(inner = inner.withMinimalLevel(level))
}
