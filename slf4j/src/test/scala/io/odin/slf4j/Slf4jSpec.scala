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

import java.util.concurrent.LinkedBlockingQueue

import io.odin.{Level, LoggerMessage, OdinSpec}

import cats.effect.unsafe.IORuntime
import cats.effect.IO
import cats.syntax.all.*
import org.scalacheck.Gen
import org.slf4j.event.Level as JLevel
import org.slf4j.event.SubstituteLoggingEvent
import org.slf4j.helpers.SubstituteLogger

class Slf4jSpec extends OdinSpec {

  private implicit val ioRuntime: IORuntime = IORuntime.global

  it should "support Slf4J loggers" in {
    val logQueue        = new LinkedBlockingQueue[SubstituteLoggingEvent]
    val subLogger       = new SubstituteLogger("subLogger", logQueue, false)
    val testSlf4JLogger = Slf4jLogger[IO](subLogger)
    testSlf4JLogger.info("test message").unsafeRunSync()
    assert(logQueue.size() == 1)
    val sentLog: SubstituteLoggingEvent = logQueue.take()
    sentLog.getMessage.contains("test message") shouldBe true
    sentLog.getLevel shouldEqual JLevel.INFO
  }

  it should "respect minLevel in the Slf4J logger" in {
    val logQueue                          = new LinkedBlockingQueue[SubstituteLoggingEvent]
    val subLogger                         = new SubstituteLogger("subLogger", logQueue, false)
    val errorSlf4JLogger                  = Slf4jLogger[IO](subLogger, Level.Error)
    val noErrorLogGen: Gen[LoggerMessage] = loggerMessageGen.filter(_.level < Level.Error)
    forAll(noErrorLogGen) { msg =>
      errorSlf4JLogger.log(msg).unsafeRunSync()
      logQueue.isEmpty shouldBe true
    }
  }

}
