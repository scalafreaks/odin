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

package io.odin.zio

import scala.util.control.{NoStackTrace, NonFatal}

/**
  * Possible errors raised by logger or during resources allocation
  */
sealed trait LoggerError extends Throwable with NoStackTrace {
  def inner: Throwable
}

case class IOException(inner: java.io.IOException) extends LoggerError

case class SecurityException(inner: java.lang.SecurityException) extends LoggerError

case class Unknown(inner: Throwable) extends LoggerError

object LoggerError {
  def apply(t: Throwable): LoggerError = t match {
    case io: java.io.IOException          => IOException(io)
    case sec: java.lang.SecurityException => SecurityException(sec)
    case NonFatal(t)                      => Unknown(t)
    case fatal                            => throw fatal
  }
}
