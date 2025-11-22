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

package io.odin.otel4s

import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import scala.concurrent.duration.FiniteDuration
import scala.util.chaining.*

import io.odin.loggers.DefaultLogger
import io.odin.meta.Position
import io.odin.Level
import io.odin.Logger
import io.odin.LoggerMessage

import cats.effect.Clock
import cats.mtl.Local
import cats.syntax.all.*
import cats.Monad
import org.typelevel.otel4s.logs.LogRecordBuilder
import org.typelevel.otel4s.logs.Logger as OLogger
import org.typelevel.otel4s.logs.LoggerProvider
import org.typelevel.otel4s.logs.Severity
import org.typelevel.otel4s.semconv.attributes.CodeAttributes
import org.typelevel.otel4s.semconv.attributes.ExceptionAttributes
import org.typelevel.otel4s.AnyValue
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.Attributes

private[otel4s] class Otel4sLogger[F[_], Ctx](
    includeFilePath: Boolean,
    minLevel: Level,
    loggerProvider: LoggerProvider[F, Ctx],
    localCtx: Local[F, Ctx]
)(
    implicit clock: Clock[F],
    F: Monad[F]
) extends DefaultLogger[F](minLevel) {

  override def withMinimalLevel(level: Level): Logger[F] =
    new Otel4sLogger[F, Ctx](includeFilePath, level, loggerProvider, localCtx)

  override def submit(msg: LoggerMessage): F[Unit] =
    for {
      logger    <- loggerProvider.logger(msg.position.packageName).withVersion(BuildInfo.version).get
      ctx       <- localCtx.ask
      severity   = Otel4sLogger.toSeverity(msg.level)
      isEnabled <- logger.meta.isEnabled(ctx, severity, None)
      observed  <- clock.realTime
      _         <- if (isEnabled) buildLogRecord(logger, observed, severity, msg, ctx).emit else F.unit
    } yield ()

  private def buildLogRecord(
      logger: OLogger[F, Ctx],
      observed: FiniteDuration,
      severity: Option[Severity],
      msg: LoggerMessage,
      context: Ctx
  ): LogRecordBuilder[F, Ctx] = {
    val builder = logger.logRecordBuilder

    severity
      .fold(builder)(builder.withSeverity(_))
      .withSeverityText(msg.level.toString())
      .withTimestamp(Instant.ofEpochMilli(msg.timestamp))
      .withObservedTimestamp(observed)
      .withBody(AnyValue.string(msg.message.value))
      .withContext(context)
      .addAttribute(Attribute("thread.name", msg.threadName))
      .addAttributes(Otel4sLogger.codeAttributes(msg.position, includeFilePath))
      .pipe { b =>
        msg.exception.fold(b)(t => b.addAttributes(Otel4sLogger.exceptionAttributes(t)))
      }
      .pipe { b =>
        if (msg.context.nonEmpty) {
          b.addAttributes(Otel4sLogger.contextAttributes(msg.context))
        } else {
          b
        }
      }
  }

}

private[otel4s] object Otel4sLogger {

  def toSeverity(l: Level): Option[Severity] =
    l match {
      case Level.Trace => Some(Severity.trace)
      case Level.Debug => Some(Severity.debug)
      case Level.Info  => Some(Severity.info)
      case Level.Warn  => Some(Severity.warn)
      case Level.Error => Some(Severity.error)
    }

  def exceptionAttributes(t: Throwable): Attributes = {
    val builder = Attributes.newBuilder

    builder += ExceptionAttributes.ExceptionType(t.getClass().getName())

    val msg = t.getMessage()
    if (msg != null) {
      builder += ExceptionAttributes.ExceptionMessage(msg)
    }

    if (t.getStackTrace().nonEmpty) {
      val sw = new StringWriter
      val pw = new PrintWriter(sw)

      t.printStackTrace(pw)

      builder += ExceptionAttributes.ExceptionStacktrace(sw.toString())
    }

    builder.result()
  }

  def codeAttributes(pos: Position, includeFilePath: Boolean): Attributes = {
    val builder = Attributes.newBuilder

    if (includeFilePath) {
      builder += CodeAttributes.CodeFilePath(pos.fileName)
    }

    builder += CodeAttributes.CodeLineNumber(pos.line.toLong)
    builder += CodeAttributes.CodeFunctionName(pos.enclosureName)
    builder += Attribute("code.namespace", pos.packageName)

    builder.result()
  }

  def contextAttributes(ctx: Map[String, String]): Attributes = {
    val builder = Attributes.newBuilder

    ctx.foreach {
      case (k, v) =>
        builder += Attribute(k, v)
    }

    builder.result()
  }

}
