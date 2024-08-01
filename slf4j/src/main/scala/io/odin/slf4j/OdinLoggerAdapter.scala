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
import org.slf4j.helpers.{FormattingTuple, MarkerIgnoringBase, MessageFormatter}
import org.slf4j.Logger

case class OdinLoggerAdapter[F[_]](loggerName: String, underlying: OdinLogger[F])(
    implicit F: Sync[F],
    dispatcher: Dispatcher[F]
) extends MarkerIgnoringBase
    with Logger
    with OdinLoggerVarargsAdapter[F] {

  override def getName: String = loggerName

  private def run(level: Level, msg: String, t: Option[Throwable] = None): Unit =
    dispatcher.unsafeRunSync(for {
      timestamp <- F.realTime
      _ <- underlying.log(
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
    } yield {
      ()
    })

  private[slf4j] def runFormatted(level: Level, tuple: FormattingTuple): Unit =
    run(level, tuple.getMessage, Option(tuple.getThrowable))

  def isTraceEnabled: Boolean = underlying.minLevel <= Level.Trace

  def trace(msg: String): Unit = run(Level.Trace, msg)

  def trace(format: String, arg: Any): Unit = runFormatted(Level.Trace, MessageFormatter.format(format, arg))

  def trace(format: String, arg1: Any, arg2: Any): Unit =
    runFormatted(Level.Trace, MessageFormatter.format(format, arg1, arg2))

  def trace(msg: String, t: Throwable): Unit =
    run(Level.Trace, msg, Option(t))

  def isDebugEnabled: Boolean = underlying.minLevel <= Level.Debug

  def debug(msg: String): Unit = run(Level.Debug, msg)

  def debug(format: String, arg: Any): Unit = runFormatted(Level.Debug, MessageFormatter.format(format, arg))

  def debug(format: String, arg1: Any, arg2: Any): Unit =
    runFormatted(Level.Debug, MessageFormatter.format(format, arg1, arg2))

  def debug(msg: String, t: Throwable): Unit =
    run(Level.Debug, msg, Option(t))

  def isInfoEnabled: Boolean = underlying.minLevel <= Level.Info

  def info(msg: String): Unit = run(Level.Info, msg)

  def info(format: String, arg: Any): Unit = runFormatted(Level.Info, MessageFormatter.format(format, arg))

  def info(format: String, arg1: Any, arg2: Any): Unit =
    runFormatted(Level.Info, MessageFormatter.format(format, arg1, arg2))

  def info(msg: String, t: Throwable): Unit =
    run(Level.Info, msg, Option(t))

  def isWarnEnabled: Boolean = underlying.minLevel <= Level.Warn

  def warn(msg: String): Unit = run(Level.Warn, msg)

  def warn(format: String, arg: Any): Unit = runFormatted(Level.Warn, MessageFormatter.format(format, arg))

  def warn(format: String, arg1: Any, arg2: Any): Unit =
    runFormatted(Level.Warn, MessageFormatter.format(format, arg1, arg2))

  def warn(msg: String, t: Throwable): Unit =
    run(Level.Warn, msg, Option(t))

  def isErrorEnabled: Boolean = underlying.minLevel <= Level.Error

  def error(msg: String): Unit = run(Level.Error, msg)

  def error(format: String, arg: Any): Unit = runFormatted(Level.Error, MessageFormatter.format(format, arg))

  def error(format: String, arg1: Any, arg2: Any): Unit =
    runFormatted(Level.Error, MessageFormatter.format(format, arg1, arg2))

  def error(msg: String, t: Throwable): Unit =
    run(Level.Error, msg, Option(t))

}
