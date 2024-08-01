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

import java.util.UUID

import io.odin.{Level, Logger, LoggerMessage, OdinSpec}

import cats.data.WriterT
import cats.effect.{Clock, IO}
import cats.effect.unsafe.IORuntime
import cats.kernel.laws.discipline.MonoidTests
import cats.syntax.all.*
import org.scalacheck.{Arbitrary, Gen}

class LoggerMonoidSpec extends OdinSpec {

  type F[A] = WriterT[IO, List[(UUID, LoggerMessage)], A]
  implicit val ioRuntime: IORuntime = IORuntime.global

  checkAll("Logger", MonoidTests[Logger[F]].monoid)

  it should "(logger1 |+| logger2).log <-> (logger1.log |+| logger2.log)" in {
    forAll { (uuid1: UUID, uuid2: UUID, msg: LoggerMessage) =>
      val logger1: Logger[F] = NamedLogger(uuid1)
      val logger2: Logger[F] = NamedLogger(uuid2)
      val a                  = (logger1 |+| logger2).log(msg)
      val b                  = logger1.log(msg) |+| logger2.log(msg)
      a.written.unsafeRunSync() shouldBe b.written.unsafeRunSync()
    }
  }

  it should "(logger1 |+| logger2).log(list) <-> (logger1.log |+| logger2.log(list))" in {
    forAll { (uuid1: UUID, uuid2: UUID, msg: List[LoggerMessage]) =>
      val logger1: Logger[F] = NamedLogger(uuid1)
      val logger2: Logger[F] = NamedLogger(uuid2)
      val a                  = (logger1 |+| logger2).log(msg)
      val b                  = logger1.log(msg) |+| logger2.log(msg)
      a.written.unsafeRunSync() shouldBe b.written.unsafeRunSync()
    }
  }

  it should "set minimal level for underlying loggers" in {
    forAll { (uuid1: UUID, uuid2: UUID, level: Level, msg: List[LoggerMessage]) =>
      val logger1: Logger[F] = NamedLogger(uuid1)
      val logger2: Logger[F] = NamedLogger(uuid2)
      val a                  = (logger1 |+| logger2).withMinimalLevel(level).log(msg)
      val b                  = (logger1.withMinimalLevel(level) |+| logger2.withMinimalLevel(level)).log(msg)
      a.written.unsafeRunSync() shouldBe b.written.unsafeRunSync()
    }
  }

  it should "respect underlying log level of each logger" in {
    forAll { (uuid1: UUID, uuid2: UUID, level1: Level, level2: Level, msg: List[LoggerMessage]) =>
      whenever(level1 != level2) {
        val logger1: Logger[F] = NamedLogger(uuid1).withMinimalLevel(level1)
        val logger2: Logger[F] = NamedLogger(uuid2).withMinimalLevel(level2)
        val logs               = msg.map(_.copy(level = level2))
        val written            = (logger1 |+| logger2).log(logs).written.unsafeRunSync()

        if (level1 > level2) {
          written shouldBe logs.tupleLeft(uuid2)
        } else {
          written shouldBe logs.tupleLeft(uuid1) ++ logs.tupleLeft(uuid2)
        }
      }
    }
  }

  case class NamedLogger(loggerId: UUID, override val minLevel: Level = Level.Trace)
      extends DefaultLogger[F](minLevel) {

    def submit(msg: LoggerMessage): F[Unit] = WriterT.tell(List(loggerId -> msg))

    def withMinimalLevel(level: Level): Logger[F] = copy(minLevel = level)

  }

  implicit def clock: Clock[IO] = zeroClock

  implicit def arbitraryWriterLogger: Arbitrary[Logger[F]] = Arbitrary(
    Gen.uuid.map(NamedLogger(_))
  )

}
