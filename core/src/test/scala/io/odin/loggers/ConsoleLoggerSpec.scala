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

package io.odin.loggers

import java.io.{ByteArrayOutputStream, PrintStream}

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.syntax.all._
import io.odin.Level._
import io.odin.formatter.Formatter
import io.odin.{Level, LoggerMessage, OdinSpec}

class ConsoleLoggerSpec extends OdinSpec {

  implicit private val ioRuntime: IORuntime = IORuntime.global

  it should "route all messages with level <= INFO to stdout" in {
    forAll { (loggerMessage: LoggerMessage, formatter: Formatter) =>
      whenever(loggerMessage.level <= Info) {
        val outBaos = new ByteArrayOutputStream()
        val stdOut = new PrintStream(outBaos)
        val errBaos = new ByteArrayOutputStream()
        val stdErr = new PrintStream(errBaos)

        val consoleLogger = ConsoleLogger[IO](formatter, stdOut, stdErr, Level.Trace)
        consoleLogger.log(loggerMessage).unsafeRunSync()
        outBaos.toString() shouldBe (formatter.format(loggerMessage) + System.lineSeparator())
      }
    }
  }

  it should "route all messages with level >= WARN to stderr" in {
    forAll { (loggerMessage: LoggerMessage, formatter: Formatter) =>
      whenever(loggerMessage.level > Info) {
        val outBaos = new ByteArrayOutputStream()
        val stdOut = new PrintStream(outBaos)
        val errBaos = new ByteArrayOutputStream()
        val stdErr = new PrintStream(errBaos)

        val consoleLogger = ConsoleLogger[IO](formatter, stdOut, stdErr, Level.Trace)
        consoleLogger.log(loggerMessage).unsafeRunSync()
        errBaos.toString() shouldBe (formatter.format(loggerMessage) + System.lineSeparator())
      }
    }
  }
}
