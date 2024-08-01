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

package io.odin.formatter

import scala.io.AnsiColor.*

case class Theme(
    reset: String,
    timestamp: String,
    context: String,
    threadName: String,
    level: String,
    position: String,
    exception: String
)

object Theme {

  val BRIGHT_BLACK = "\u001b[30;1m"

  val ansi: Theme = Theme(
    reset = RESET,
    timestamp = WHITE,
    context = MAGENTA,
    threadName = GREEN,
    level = BRIGHT_BLACK,
    position = BLUE,
    exception = RED
  )

}
