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

import io.odin.{Level, Logger as OdinLogger, LoggerMessage}
import io.odin.meta.Position

import cats.effect.kernel.Sync
import cats.effect.std.Dispatcher
import cats.syntax.all.*
import cats.Eval
import org.slf4j.{event, Marker}
import org.slf4j.helpers.{LegacyAbstractLogger, MessageFormatter}

case class OdinLoggerAdapter[F[_]](loggerName: String, underlying: OdinLogger[F])(
    implicit F: Sync[F],
    dispatcher: Dispatcher[F]
) extends LegacyAbstractLogger {

  override def getName: String = loggerName

  override def getFullyQualifiedCallerName: String = null

  override def handleNormalizedLoggingCall(
      level: event.Level,
      marker: Marker,
      messagePattern: String,
      arguments: Array[AnyRef],
      throwable: Throwable
  ): Unit =
    run(
      level match {
        case event.Level.ERROR => Level.Error
        case event.Level.WARN  => Level.Warn
        case event.Level.INFO  => Level.Info
        case event.Level.DEBUG => Level.Debug
        case event.Level.TRACE => Level.Trace
      },
      MessageFormatter.arrayFormat(messagePattern, arguments, throwable).getMessage,
      Option(throwable)
    )

  private def run(level: Level, msg: String, t: Option[Throwable]): Unit =
    dispatcher.unsafeRunSync {
      F.realTime.flatMap { timestamp =>
        underlying.log(
          LoggerMessage(
            level = level,
            message = Eval.now(msg),
            context = Map.empty,
            exception = t,
            position = Position(
              fileName = loggerName,
              enclosureName = loggerName,
              packageName = loggerName,
              line = -1
            ),
            threadName = Thread.currentThread().getName,
            timestamp = timestamp.toMillis
          )
        )
      }
    }

  def isTraceEnabled: Boolean = underlying.minLevel <= Level.Trace

  def isDebugEnabled: Boolean = underlying.minLevel <= Level.Debug

  def isInfoEnabled: Boolean = underlying.minLevel <= Level.Info

  def isWarnEnabled: Boolean = underlying.minLevel <= Level.Warn

  def isErrorEnabled: Boolean = underlying.minLevel <= Level.Error

}
