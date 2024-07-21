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
import cats.data.WriterT
import cats.effect.kernel.Clock
import io.odin.{Level, Logger, LoggerMessage}

/**
  * Pure logger that stores logs in `WriterT` log
  */
class WriterTLogger[F[_]: Clock: Monad](override val minLevel: Level = Level.Trace)
    extends DefaultLogger[WriterT[F, List[LoggerMessage], *]](minLevel) {
  def submit(msg: LoggerMessage): WriterT[F, List[LoggerMessage], Unit] = WriterT.tell(List(msg))

  override def submit(msgs: List[LoggerMessage]): WriterT[F, List[LoggerMessage], Unit] = WriterT.tell(msgs)

  def withMinimalLevel(level: Level): Logger[WriterT[F, List[LoggerMessage], *]] = new WriterTLogger[F](level)
}
