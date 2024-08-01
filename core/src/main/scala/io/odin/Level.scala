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

import cats.{Order, Show}
import cats.kernel.{LowerBounded, PartialOrder, UpperBounded}

/**
  * Message log level
  */
sealed trait Level

object Level {

  case object Trace extends Level {
    override val toString: String = "TRACE"
  }

  case object Debug extends Level {
    override val toString: String = "DEBUG"
  }

  case object Info extends Level {
    override val toString: String = "INFO"
  }

  case object Warn extends Level {
    override val toString: String = "WARN"
  }

  case object Error extends Level {
    override val toString: String = "ERROR"
  }

  implicit val show: Show[Level] = Show.fromToString[Level]

  implicit val order: Order[Level] & LowerBounded[Level] & UpperBounded[Level] =
    new Order[Level] with LowerBounded[Level] with UpperBounded[Level] { self =>
      private def f: Level => Int = {
        case Error => 4
        case Warn  => 3
        case Info  => 2
        case Debug => 1
        case Trace => 0
      }

      def compare(x: Level, y: Level): Int = f(x).compare(f(y))

      def minBound: Level = Level.Trace

      def maxBound: Level = Level.Error

      def partialOrder: PartialOrder[Level] = self
    }

}
