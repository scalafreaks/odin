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
import scala.concurrent.duration.*

import io.odin.Level
import io.odin.LoggerMessage
import io.odin.OdinSpec

import cats.effect.unsafe.IORuntime
import cats.effect.IO
import cats.syntax.order.*
import org.scalatest.OptionValues
import org.typelevel.otel4s.logs.LoggerProvider
import org.typelevel.otel4s.sdk.context.Context
import org.typelevel.otel4s.sdk.testkit.OpenTelemetrySdkTestkit
import org.typelevel.otel4s.semconv.attributes.CodeAttributes
import org.typelevel.otel4s.semconv.attributes.ExceptionAttributes
import org.typelevel.otel4s.AnyValue
import org.typelevel.otel4s.Attribute

class Otel4sLoggerSpec extends OdinSpec with OptionValues {

  private implicit val ioRuntime: IORuntime = IORuntime.global

  it should "route all messages to otel4s" in {
    forAll { (loggerMessage: LoggerMessage) =>
      OpenTelemetrySdkTestkit
        .inMemory[IO]()
        .use { testkit =>
          val loggerProvider = testkit.loggerProvider
          val localCtx       = testkit.localContext
          val otel4sLogger   = new Otel4sLogger[IO, Context](false, Level.Trace, loggerProvider, localCtx)
          for {
            now        <- IO.realTime
            _          <- otel4sLogger.log(loggerMessage)
            logRecords <- testkit.collectLogs
          } yield {
            logRecords should have length 1
            val logRecord = logRecords.head

            // basic log record properties
            logRecord.severity shouldBe Otel4sLogger.toSeverity(loggerMessage.level)
            logRecord.body shouldBe Some(AnyValue.string(loggerMessage.message.value))

            // timing properties
            logRecord.timestamp shouldBe Some(loggerMessage.timestamp.millis)
            logRecord.observedTimestamp should be > now

            // trace context properties
            logRecord.traceContext shouldBe empty

            val attributes = logRecord.attributes.elements

            // error related attributes
            val exceptionName = loggerMessage.exception.map(_.getClass().getName())

            val exceptionMessage = loggerMessage.exception.map(_.getMessage()).filter(_ != null)

            val stackTrace = loggerMessage.exception.map { t =>
              val writer  = new StringWriter
              val printer = new PrintWriter(writer)
              t.printStackTrace(printer)
              writer.toString()
            }

            attributes.get(ExceptionAttributes.ExceptionType) shouldBe ExceptionAttributes.ExceptionType
              .maybe(exceptionName)

            attributes.get(ExceptionAttributes.ExceptionMessage) shouldBe ExceptionAttributes.ExceptionMessage
              .maybe(exceptionMessage)

            attributes.get(ExceptionAttributes.ExceptionStacktrace) shouldBe ExceptionAttributes.ExceptionStacktrace
              .maybe(stackTrace)

            // location related attributes
            attributes.get(CodeAttributes.CodeFunctionName) shouldBe Some(
              CodeAttributes.CodeFunctionName(loggerMessage.position.enclosureName)
            )
            attributes.get[String]("code.namespace") shouldBe Some(
              Attribute("code.namespace", loggerMessage.position.packageName)
            )

          }
        }
        .unsafeRunSync()
    }
  }

  it should "not send log messages lower than the configured minimum level" in {
    forAll { (loggerMessage: LoggerMessage) =>
      whenever(loggerMessage.level < Level.Warn) {
        OpenTelemetrySdkTestkit
          .inMemory[IO]()
          .use { testkit =>
            val loggerProvider = testkit.loggerProvider
            val localCtx       = testkit.localContext
            val otel4sLogger   = new Otel4sLogger[IO, Context](false, Level.Warn, loggerProvider, localCtx)
            for {
              _          <- otel4sLogger.log(loggerMessage)
              logRecords <- testkit.collectLogs
            } yield logRecords should have length 0
          }
          .unsafeRunSync()
      }
    }
  }

  it should "not send any log messages if the otel4s backend is disabled" in {
    forAll { (loggerMessage: LoggerMessage) =>
      OpenTelemetrySdkTestkit
        .inMemory[IO]()
        .use { testkit =>
          val loggerProvider = LoggerProvider.noop[IO, Context]
          val localCtx       = testkit.localContext
          val otel4sLogger   = new Otel4sLogger[IO, Context](false, Level.Trace, loggerProvider, localCtx)
          for {
            _          <- otel4sLogger.log(loggerMessage)
            logRecords <- testkit.collectLogs
          } yield logRecords should have length 0
        }
        .unsafeRunSync()
    }
  }

  it should "propagate tracing context to logs" in {
    forAll { (loggerMessage: LoggerMessage) =>
      OpenTelemetrySdkTestkit
        .inMemory[IO]()
        .use { testkit =>
          val loggerProvider = testkit.loggerProvider
          val localCtx       = testkit.localContext
          val otel4sLogger   = new Otel4sLogger[IO, Context](false, Level.Trace, loggerProvider, localCtx)
          for {
            tracer     <- testkit.tracerProvider.tracer("test").get
            _          <- tracer.span("test.span").surround(otel4sLogger.log(loggerMessage))
            logRecords <- testkit.collectLogs
          } yield {
            logRecords should have length 1

            val logRecord = logRecords.head

            logRecord.traceContext shouldBe defined
          }
        }
        .unsafeRunSync()
    }
  }

}
