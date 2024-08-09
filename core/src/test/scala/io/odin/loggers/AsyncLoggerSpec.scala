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

import scala.concurrent.duration.*

import io.odin.{Level, Logger, LoggerMessage, OdinSpec}
import io.odin.syntax.*

import cats.effect.{IO, Ref, Resource}
import cats.effect.std.Queue
import cats.effect.testkit.*
import cats.effect.unsafe.IORuntime
import cats.syntax.all.*

class AsyncLoggerSpec extends OdinSpec {

  private implicit val ioRuntime: IORuntime = IORuntime.global

  case class RefLogger(ref: Ref[IO, List[LoggerMessage]], override val minLevel: Level = Level.Trace)
      extends DefaultLogger[IO](minLevel) {

    def submit(msg: LoggerMessage): IO[Unit] = ref.update(_ :+ msg)

    override def submit(msgs: List[LoggerMessage]): IO[Unit] = ref.update(_ ::: msgs)

    def withMinimalLevel(level: Level): Logger[IO] = copy(minLevel = level)

  }

  it should "push logs down the chain" in {
    forAll { (msgs: List[LoggerMessage]) =>
      TestControl
        .executeEmbed {
          (for {
            ref    <- Resource.eval(IO.ref(List.empty[LoggerMessage]))
            logger <- RefLogger(ref).withAsync()
            _      <- Resource.eval(msgs.traverse(logger.log))
            before <- Resource.eval(ref.get)
            _      <- Resource.eval(IO.sleep(2.millis))
            after  <- Resource.eval(ref.get)
          } yield {
            before shouldBe empty
            after shouldBe msgs
          }).use(IO(_))
        }
        .unsafeRunSync()
    }
  }

  it should "push log operations to the queue" in {
    forAll { (msgs: List[LoggerMessage]) =>
      (for {
        queue    <- Queue.unbounded[IO, IO[Unit]]
        ref      <- IO.ref(List.empty[LoggerMessage])
        logger    = new AsyncLogger(queue, RefLogger(ref).withMinimalLevel(Level.Trace)).withMinimalLevel(Level.Trace)
        _        <- msgs.traverse(logger.log)
        enqueued <- List.fill(msgs.length)(queue.take).sequence
        reportedBefore <- ref.get
        _              <- enqueued.sequence_
        reportedAfter  <- ref.get
      } yield {
        enqueued should have length msgs.length.toLong
        reportedBefore shouldBe empty
        reportedAfter shouldBe msgs
      }).unsafeRunSync()
    }
  }

  it should "ignore errors in underlying logger" in {
    val errorLogger = new DefaultLogger[IO](Level.Trace) {
      def submit(msg: LoggerMessage): IO[Unit] = IO.raiseError(new Error)

      def withMinimalLevel(level: Level): Logger[IO] = this
    }
    forAll { (msgs: List[LoggerMessage]) =>
      (for {
        queue  <- Queue.unbounded[IO, IO[Unit]]
        logger  = new AsyncLogger(queue, errorLogger)
        _      <- logger.log(msgs)
        result <- logger.drain
      } yield {
        result shouldBe ()
      }).unsafeRunSync()
    }
  }

  it should "respect updated minimal level" in {
    forAll { (msgs: List[LoggerMessage]) =>
      TestControl
        .executeEmbed {
          for {
            infoMessages <- IO.pure(msgs.filter(_.level >= Level.Info))
            ref          <- IO.ref(List.empty[LoggerMessage])
            _ <- RefLogger(ref, Level.Info).withAsync().use { logger =>
                   val traceLogger = logger.withMinimalLevel(Level.Trace)
                   for {
                     _ <- msgs.traverse(logger.log)      // only Info, Warn, Error messages should be logged
                     _ <- msgs.traverse(traceLogger.log) // all messages should be logged
                   } yield ()
                 }
            reported <- ref.get // TestControl ensures periodic flush is not run, only resource close flush
          } yield {
            val (infos, all) = reported.splitAt(infoMessages.size)
            infos shouldBe infoMessages
            all shouldBe msgs
          }
        }
        .unsafeRunSync()
    }
  }

}
