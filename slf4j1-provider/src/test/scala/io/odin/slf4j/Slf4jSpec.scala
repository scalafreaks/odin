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

import scala.collection.immutable.Queue

import io.odin.{Level, LoggerMessage, OdinSpec}

import cats.effect.kernel.Ref
import cats.effect.unsafe.IORuntime
import cats.effect.IO
import cats.syntax.all.*
import org.slf4j.{Logger, LoggerFactory}

class Slf4jSpec extends OdinSpec {

  private implicit val ioRuntime: IORuntime = IORuntime.global

  it should "log with correct level" in {
    forAll { (msgs: List[LoggerMessage]) =>
      val (logger, buffer) = getLogger
      msgs.foreach { msg =>
        msg.level match {
          case Level.Trace => logger.trace(msg.message.value)
          case Level.Debug => logger.debug(msg.message.value)
          case Level.Info  => logger.info(msg.message.value)
          case Level.Warn  => logger.warn(msg.message.value)
          case Level.Error => logger.error(msg.message.value)
        }
      }

      buffer.get.unsafeRunSync().map(msg => (msg.message.value, msg.level)) shouldBe msgs.map(msg =>
        (msg.message.value, msg.level)
      )
    }

  }

  it should "log exceptions" in {
    forAll { (msgs: List[LoggerMessage], t: Throwable) =>
      val (logger, buffer) = getLogger
      msgs.foreach { msg =>
        msg.level match {
          case Level.Trace => logger.trace(msg.message.value, t)
          case Level.Debug => logger.debug(msg.message.value, t)
          case Level.Info  => logger.info(msg.message.value, t)
          case Level.Warn  => logger.warn(msg.message.value, t)
          case Level.Error => logger.error(msg.message.value, t)
        }
      }

      buffer.get.unsafeRunSync().map(msg => (msg.message.value, msg.level, msg.exception)) shouldBe msgs.map(msg =>
        (msg.message.value, msg.level, Some(t))
      )
    }
  }

  it should "resolve minimal level" in {
    LoggerFactory.getLogger(Level.Trace.toString).isTraceEnabled shouldBe true

    LoggerFactory.getLogger(Level.Debug.toString).isDebugEnabled shouldBe true
    LoggerFactory.getLogger(Level.Debug.toString).isTraceEnabled shouldBe false

    LoggerFactory.getLogger(Level.Info.toString).isInfoEnabled shouldBe true
    LoggerFactory.getLogger(Level.Info.toString).isDebugEnabled shouldBe false

    LoggerFactory.getLogger(Level.Warn.toString).isWarnEnabled shouldBe true
    LoggerFactory.getLogger(Level.Warn.toString).isInfoEnabled shouldBe false

    LoggerFactory.getLogger(Level.Error.toString).isErrorEnabled shouldBe true
    LoggerFactory.getLogger(Level.Error.toString).isWarnEnabled shouldBe false
  }

  it should "format logs" in {
    forAll { (msgs: List[LoggerMessage]) =>
      val (logger, buffer) = getLogger
      msgs.foreach { msg =>
        msg.level match {
          case Level.Trace => logger.trace("{}", msg.message.value)
          case Level.Debug => logger.debug("{}", msg.message.value)
          case Level.Info  => logger.info("{}", msg.message.value)
          case Level.Warn  => logger.warn("{}", msg.message.value)
          case Level.Error => logger.error("{}", msg.message.value)
        }
      }

      buffer.get.unsafeRunSync().map(msg => (msg.message.value, msg.level)) shouldBe msgs.map(msg =>
        (msg.message.value, msg.level)
      )
    }
  }

  it should "format logs with two arguments" in {
    forAll { (msgs: List[LoggerMessage], i: String) =>
      val (logger, buffer) = getLogger
      msgs.foreach { msg =>
        msg.level match {
          case Level.Trace => logger.trace("{} {}", msg.message.value: Any, i: Any)
          case Level.Debug => logger.debug("{} {}", msg.message.value: Any, i: Any)
          case Level.Info  => logger.info("{} {}", msg.message.value: Any, i: Any)
          case Level.Warn  => logger.warn("{} {}", msg.message.value: Any, i: Any)
          case Level.Error => logger.error("{} {}", msg.message.value: Any, i: Any)
        }
      }

      buffer.get.unsafeRunSync().map(msg => (msg.message.value, msg.level)) shouldBe msgs.map(msg =>
        (s"${msg.message.value} $i", msg.level)
      )
    }
  }

  it should "format logs with multiple arguments" in {
    forAll { (msgs: List[LoggerMessage], i: String, i2: String) =>
      val (logger, buffer) = getLogger
      msgs.foreach { msg =>
        msg.level match {
          case Level.Trace => logger.trace("{} {} {}", msg.message.value, i, i2)
          case Level.Debug => logger.debug("{} {} {}", msg.message.value, i, i2)
          case Level.Info  => logger.info("{} {} {}", msg.message.value, i, i2)
          case Level.Warn  => logger.warn("{} {} {}", msg.message.value, i, i2)
          case Level.Error => logger.error("{} {} {}", msg.message.value, i, i2)
        }
      }

      buffer.get.unsafeRunSync().map(msg => (msg.message.value, msg.level)) shouldBe msgs.map(msg =>
        (s"${msg.message.value} $i $i2", msg.level)
      )
    }
  }

  it should "not log messages with level lower than set" in {
    forAll { (msgs: List[LoggerMessage], minLevel: Level) =>
      val expected         = msgs.filter(_.level >= minLevel)
      val (logger, buffer) = {
        val l = LoggerFactory.getLogger(minLevel.show).asInstanceOf[OdinLoggerAdapter[IO]]
        (l, l.underlying.asInstanceOf[BufferingLogger[IO]].buffer)
      }

      msgs.foreach { msg =>
        msg.level match {
          case Level.Trace => logger.trace(msg.message.value)
          case Level.Debug => logger.debug(msg.message.value)
          case Level.Info  => logger.info(msg.message.value)
          case Level.Warn  => logger.warn(msg.message.value)
          case Level.Error => logger.error(msg.message.value)
        }
      }

      buffer.get.unsafeRunSync().size shouldBe expected.size
    }
  }

  def getLogger: (Logger, Ref[IO, Queue[LoggerMessage]]) = {
    val logger = LoggerFactory.getLogger(this.getClass).asInstanceOf[OdinLoggerAdapter[IO]]
    (logger, logger.underlying.asInstanceOf[BufferingLogger[IO]].buffer)
  }

}
