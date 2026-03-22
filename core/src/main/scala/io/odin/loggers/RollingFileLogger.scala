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

import java.nio.file.{Files, OpenOption, Path, Paths}
import java.time.LocalDateTime
import java.util.TimeZone
import scala.concurrent.duration.*

import io.odin.{Level, Logger, LoggerMessage}
import io.odin.formatter.Formatter

import cats.effect.kernel.*
import cats.effect.std.NonEmptyHotswap
import cats.syntax.all.*
import cats.Functor

object RollingFileLogger {

  private type RolloverSignal[F[_]] = Deferred[F, Unit]
  private type RolloverLogger[F[_]] = (Logger[F], RolloverSignal[F])

  def apply[F[_]](
      fileNamePattern: LocalDateTime => String,
      maxFileSizeInBytes: Option[Long],
      rolloverInterval: Option[FiniteDuration],
      formatter: Formatter,
      minLevel: Level,
      openOptions: Seq[OpenOption] = Seq.empty
  )(implicit F: Async[F]): Resource[F, Logger[F]] = {

    def rollingLogger =
      new RollingFileLoggerFactory(
        fileNamePattern,
        maxFileSizeInBytes,
        rolloverInterval,
        formatter,
        minLevel,
        FileLogger.apply[F],
        openOptions
      ).mk

    def fileLogger =
      Resource.suspend {
        for {
          localTime <- localDateTimeNow
        } yield FileLogger[F](fileNamePattern(localTime), formatter, minLevel, openOptions)
      }

    if (maxFileSizeInBytes.isDefined || rolloverInterval.isDefined) rollingLogger else fileLogger
  }

  private[odin] final class RolloverFileLogger[F[_]: Clock: MonadCancelThrow](
      current: NonEmptyHotswap[F, RolloverLogger[F]],
      override val minLevel: Level
  ) extends DefaultLogger[F](minLevel) {

    def withMinimalLevel(level: Level): Logger[F] = new RolloverFileLogger(current, level)

    def submit(msg: LoggerMessage): F[Unit] = current.get.use { case (logger, _) => logger.log(msg) }

    override def submit(msgs: List[LoggerMessage]): F[Unit] = current.get.use { case (logger, _) => logger.log(msgs) }

  }

  private[odin] final class RollingFileLoggerFactory[F[_]](
      fileNamePattern: LocalDateTime => String,
      maxFileSizeInBytes: Option[Long],
      rolloverInterval: Option[FiniteDuration],
      formatter: Formatter,
      minLevel: Level,
      underlyingLogger: (String, Formatter, Level, Seq[OpenOption]) => Resource[F, Logger[F]],
      openOptions: Seq[OpenOption]
  )(implicit F: Async[F]) {

    def mk: Resource[F, Logger[F]] =
      for {
        hs <- NonEmptyHotswap[F, RolloverLogger[F]](allocate)
        _  <- F.background(rollingLoop(hs))
      } yield new RolloverFileLogger(hs, minLevel)

    private def now: F[Long] = F.realTime.map(_.toMillis)

    /**
      * Create file logger along with the file watcher
      */
    private def allocate: Resource[F, RolloverLogger[F]] =
      Resource.suspend(localDateTimeNow.map { localTime =>
        val fileName = fileNamePattern(localTime)
        underlyingLogger(fileName, formatter, minLevel, openOptions).product(fileWatcher(Paths.get(fileName)))
      })

    /**
      * Create resource with fiber that's canceled on resource release.
      *
      * Fiber itself is a file watcher that checks if rollover interval or size are not exceeded and finishes it work
      * the moment at least one of those conditions is met.
      */
    private def fileWatcher(filePath: Path): Resource[F, RolloverSignal[F]] = {
      val checkFileSize: Long => Boolean =
        maxFileSizeInBytes match {
          case Some(max) => fileSize => fileSize >= max
          case _         => _ => false
        }

      val checkRolloverInterval: (Long, Long) => Boolean =
        rolloverInterval match {
          case Some(finite: FiniteDuration) =>
            (start, now) => start + finite.toMillis <= now
          case _ =>
            (_, _) => false
        }

      def checkConditions(start: Long, now: Long, fileSize: Long): Boolean =
        checkFileSize(fileSize) || checkRolloverInterval(start, now)

      def loop(start: Long): F[Unit] =
        for {
          time <- now
          size <- maxFileSizeInBytes.fold(F.pure(0L))(_ => F.blocking(Files.size(filePath)))
          _    <- F.unlessA(checkConditions(start, time, size))(F.delayBy(loop(start), 100.millis))
        } yield ()

      for {
        rolloverSignal <- Resource.eval(Deferred[F, Unit])
        _              <- F.background(now >>= loop >>= rolloverSignal.complete)
      } yield rolloverSignal
    }

    /**
      * Once rollover signal is sent, it means that it's triggered and current logger's file exceeded TTL or allowed
      * size. At this moment new logger, new watcher and new release values shall be allocated to replace the old ones.
      *
      * Once new values are allocated and corresponding references are updated, run the old release and loop the whole
      * function using new watcher
      */
    private def rollingLoop(hs: NonEmptyHotswap[F, RolloverLogger[F]]): F[Unit] =
      F.foreverM(hs.get.use { case (_, signal) => signal.get } >> hs.swap(allocate))

  }

  private def localDateTimeNow[F[_]: Functor](implicit clock: Clock[F]): F[LocalDateTime] =
    clock.realTimeInstant.map(LocalDateTime.ofInstant(_, TimeZone.getDefault.toZoneId))

}
