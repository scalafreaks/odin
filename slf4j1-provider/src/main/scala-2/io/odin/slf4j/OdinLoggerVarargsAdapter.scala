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

import io.odin.Level

import org.slf4j.helpers.MessageFormatter

trait OdinLoggerVarargsAdapter[F[_]] { self: OdinLoggerAdapter[F] =>

  def trace(format: String, arguments: AnyRef*): Unit =
    runFormatted(Level.Trace, MessageFormatter.arrayFormat(format, arguments.toArray))

  def debug(format: String, arguments: AnyRef*): Unit =
    runFormatted(Level.Debug, MessageFormatter.arrayFormat(format, arguments.toArray))

  def info(format: String, arguments: AnyRef*): Unit =
    runFormatted(Level.Info, MessageFormatter.arrayFormat(format, arguments.toArray))

  def warn(format: String, arguments: AnyRef*): Unit =
    runFormatted(Level.Warn, MessageFormatter.arrayFormat(format, arguments.toArray))

  def error(format: String, arguments: AnyRef*): Unit =
    runFormatted(Level.Error, MessageFormatter.arrayFormat(format, arguments.toArray))

}
