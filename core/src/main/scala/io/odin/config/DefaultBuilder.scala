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

package io.odin.config

import io.odin.Logger

import cats.Applicative

class DefaultBuilder[F[_]: Applicative](val withDefault: Logger[F] => Logger[F]) {

  def withNoopFallback: Logger[F] =
    withDefault(Logger.noop[F])

  def withFallback(fallback: Logger[F]): Logger[F] =
    withDefault(fallback)

}
