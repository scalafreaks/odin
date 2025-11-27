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

package io.odin

import cats.effect.Clock
import cats.mtl.Local
import cats.Monad
import org.typelevel.otel4s.logs.LoggerProvider

package object otel4s {

  /**
    * Create logger that sends log entries to opentelemetry via otel4s.
    *
    * @param includeFilePath whether to log the absolute source file path
    * @param minLevel minimal level of logs to be recorded
    */
  def otel4sLogger[F[_]: Clock: Monad, Ctx](
      includeFilePath: Boolean = false,
      minLevel: Level = Level.Trace
  )(implicit loggerProvider: LoggerProvider[F, Ctx], localCtx: Local[F, Ctx]): Logger[F] =
    new Otel4sLogger[F, Ctx](includeFilePath, minLevel, loggerProvider, localCtx)

}
