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

import cats.effect.kernel.Ref
import cats.effect.Sync
import io.odin.loggers.DefaultLogger
import io.odin.{Level, Logger, LoggerMessage}

import scala.collection.immutable.Queue

case class BufferingLogger[F[_]](override val minLevel: Level)(implicit F: Sync[F]) extends DefaultLogger[F](minLevel) {

  val buffer: Ref[F, Queue[LoggerMessage]] = Ref.unsafe[F, Queue[LoggerMessage]](Queue.empty)

  def submit(msg: LoggerMessage): F[Unit] = buffer.update(_.enqueue(msg))

  def withMinimalLevel(level: Level): Logger[F] = copy(minLevel = level)
}
