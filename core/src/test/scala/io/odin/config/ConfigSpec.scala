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

package io.odin.config

import io.odin.{Level, Logger, LoggerMessage, OdinSpec}
import io.odin.loggers.DefaultLogger

import cats.data.WriterT
import cats.effect.unsafe.IORuntime
import cats.effect.IO
import cats.syntax.all.*

class ConfigSpec extends OdinSpec {

  implicit private val ioRuntime: IORuntime = IORuntime.global

  type F[A] = WriterT[IO, List[(String, LoggerMessage)], A]

  case class TestLogger(loggerName: String, override val minLevel: Level = Level.Trace)
      extends DefaultLogger[F](minLevel) {

    def submit(msg: LoggerMessage): F[Unit] = WriterT.tell(List(loggerName -> msg))

    def withMinimalLevel(level: Level): Logger[F] = copy(minLevel = level)

  }

  it should "route based on the package" in {
    forAll { (ls: List[LoggerMessage]) =>
      val withEnclosure = ls.groupBy(_.position.enclosureName)
      val routerLogger =
        enclosureRouting[F](
          withEnclosure.toList.map {
            case (key, _) =>
              key -> TestLogger(key)
          }*
        ).withNoopFallback.withMinimalLevel(Level.Trace)

      val written      = ls.traverse(routerLogger.log).written.unsafeRunSync()
      val batchWritten = routerLogger.log(ls).written.unsafeRunSync()

      written shouldBe ls.map(msg => msg.position.enclosureName -> msg)
      batchWritten should contain theSameElementsAs written
    }
  }

  it should "route based on the class" in {
    forAll(nonEmptyStringGen, nonEmptyStringGen, nonEmptyStringGen) {
      (msg: String, loggerName1: String, loggerName2: String) =>
        val routerLogger =
          classRouting[F](
            classOf[ConfigSpec]   -> TestLogger(loggerName1),
            classOf[TestClass[F]] -> TestLogger(loggerName2)
          ).withNoopFallback

        val List((ln1, _)) = routerLogger.info(msg).written.unsafeRunSync(): @unchecked
        val List((ln2, _)) = (new TestClass[F](routerLogger)).log(msg).written.unsafeRunSync(): @unchecked

        ln1 shouldBe loggerName1
        ln2 shouldBe loggerName2
    }
  }

  it should "route based on the level" in {
    forAll { (ls: List[LoggerMessage]) =>
      val withLevels = ls.groupBy(_.level)
      val routerLogger = levelRouting[F](
        withLevels.map {
          case (key, _) =>
            key -> TestLogger(key.show)
        }
      ).withNoopFallback.withMinimalLevel(Level.Trace)

      val written      = ls.traverse(routerLogger.log).written.unsafeRunSync()
      val batchWritten = routerLogger.log(ls).written.unsafeRunSync()

      written.toMap shouldBe ls.map(msg => msg.level.show -> msg).toMap
      batchWritten should contain theSameElementsAs written
    }
  }

  it should "fallback to provided logger" in {
    forAll { (ls: List[LoggerMessage]) =>
      val fallback     = TestLogger("fallback")
      val routerLogger = enclosureRouting[F]().withFallback(fallback).withMinimalLevel(Level.Trace)

      val written      = ls.traverse(routerLogger.log).written.unsafeRunSync()
      val batchWritten = routerLogger.log(ls).written.unsafeRunSync()

      written shouldBe ls.map(msg => "fallback" -> msg)
      batchWritten should contain theSameElementsAs written
    }
  }

  it should "not fallback to provided logger when message level does not match mapped logger level" in {
    forAll { (head: LoggerMessage, ls: List[LoggerMessage]) =>
      val infoHead  = head.copy(level = Level.Info)
      val enclosure = infoHead.position.enclosureName

      val fallback = TestLogger("fallback")
      val routed   = TestLogger("underlying").withMinimalLevel(Level.Error)
      val routerLogger =
        enclosureRouting[F](enclosure -> routed).withFallback(fallback)

      val written      = (infoHead :: ls).traverse(routerLogger.log).written.unsafeRunSync()
      val batchWritten = routerLogger.log(infoHead :: ls).written.unsafeRunSync()

      written shouldBe ls.map(msg => "fallback" -> msg)
      batchWritten should contain theSameElementsAs written
    }
  }

  it should "check underlying min level" in {
    forAll { (minLevel: Level, ls: List[LoggerMessage]) =>
      val underlying = TestLogger("underlying").withMinimalLevel(minLevel)
      val logger     = enclosureRouting("" -> underlying).withNoopFallback

      val written      = ls.traverse(logger.log).written.unsafeRunSync().map(_._2)
      val batchWritten = logger.log(ls).written.unsafeRunSync().map(_._2)

      written shouldBe ls.filter(_.level >= minLevel)
      batchWritten should contain theSameElementsAs written
    }
  }

  it should "check fallback min level" in {
    forAll { (minLevel: Level, ls: List[LoggerMessage]) =>
      val fallback = TestLogger("fallback").withMinimalLevel(minLevel)
      val logger   = enclosureRouting[F]().withFallback(fallback)

      val written      = ls.traverse(logger.log).written.unsafeRunSync().map(_._2)
      val batchWritten = logger.log(ls).written.unsafeRunSync().map(_._2)

      written shouldBe ls.filter(_.level >= minLevel)
      batchWritten should contain theSameElementsAs written
    }
  }

}

class TestClass[F[_]](logger: Logger[F]) {

  def log(msg: String): F[Unit] = logger.info(msg)

}
