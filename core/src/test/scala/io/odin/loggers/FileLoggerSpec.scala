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

import java.nio.file.{Files, Path, Paths}
import java.util.UUID
import scala.concurrent.duration.*

import io.odin.*
import io.odin.{LoggerMessage, OdinSpec}
import io.odin.formatter.Formatter

import cats.effect.{IO, Resource}
import cats.effect.unsafe.IORuntime

class FileLoggerSpec extends OdinSpec {

  implicit private val ioRuntime: IORuntime = IORuntime.global

  private val fileResource = Resource.make[IO, Path] {
    IO.delay(Files.createTempFile(UUID.randomUUID().toString, ""))
  } { file =>
    IO.delay(Files.delete(file))
  }

  it should "write formatted message into file" in {
    forAll { (loggerMessage: LoggerMessage, formatter: Formatter) =>
      (for {
        path    <- fileResource
        fileName = path.toString
        logger  <- FileLogger[IO](fileName, formatter, Level.Trace, Nil)
        _       <- Resource.eval(logger.log(loggerMessage))
      } yield {
        new String(Files.readAllBytes(Paths.get(fileName))) shouldBe formatter.format(loggerMessage) + lineSeparator
      }).use(IO(_))
        .unsafeRunSync()
    }
  }

  it should "write formatted messages into file" in {
    forAll { (loggerMessage: List[LoggerMessage], formatter: Formatter) =>
      (for {
        path    <- fileResource
        fileName = path.toString
        logger  <- FileLogger[IO](fileName, formatter, Level.Trace, Nil)
        _       <- Resource.eval(logger.log(loggerMessage))
      } yield {
        new String(Files.readAllBytes(Paths.get(fileName))) shouldBe loggerMessage
          .map(formatter.format)
          .mkString(lineSeparator) + (if (loggerMessage.isEmpty) "" else lineSeparator)
      }).use(IO(_))
        .unsafeRunSync()
    }
  }

  it should "write in async mode" in {
    forAll { (loggerMessage: List[LoggerMessage], formatter: Formatter) =>
      (for {
        path    <- fileResource
        fileName = path.toString
        logger  <- asyncFileLogger[IO](fileName, formatter)
        _       <- Resource.eval(logger.withMinimalLevel(Level.Trace).log(loggerMessage))
        _       <- Resource.eval(IO.sleep(2.seconds))
      } yield {
        new String(Files.readAllBytes(Paths.get(fileName))) shouldBe loggerMessage
          .map(formatter.format)
          .mkString(lineSeparator) + (if (loggerMessage.isEmpty) "" else lineSeparator)
      }).use(IO(_))
        .unsafeRunSync()
    }
  }

}
