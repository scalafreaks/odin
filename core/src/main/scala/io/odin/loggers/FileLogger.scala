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

import java.io.BufferedWriter
import java.nio.file.{Files, OpenOption, Paths}

import io.odin.{Level, Logger, LoggerMessage}
import io.odin.formatter.Formatter

import cats.effect.kernel.{Resource, Sync}
import cats.syntax.all.*

/**
  * Write to given log writer with provided formatter
  */
private[loggers] final class FileLogger[F[_]](
    buffer: BufferedWriter,
    formatter: Formatter,
    override val minLevel: Level
)(
    implicit F: Sync[F]
) extends DefaultLogger[F](minLevel) {

  def withMinimalLevel(level: Level): Logger[F] = new FileLogger(buffer, formatter, level)

  def submit(msg: LoggerMessage): F[Unit] =
    F.guarantee(write(msg, formatter), flush)

  override def submit(msgs: List[LoggerMessage]): F[Unit] =
    F.guarantee(msgs.traverse_(write(_, formatter)), flush)

  private def write(msg: LoggerMessage, formatter: Formatter): F[Unit] =
    F.blocking {
      buffer.write(formatter.format(msg) + System.lineSeparator())
    }

  private def flush: F[Unit] = F.delay(buffer.flush()).voidError

}

object FileLogger {

  def apply[F[_]](
      fileName: String,
      formatter: Formatter,
      minLevel: Level,
      openOptions: Seq[OpenOption] = Seq.empty
  )(
      implicit F: Sync[F]
  ): Resource[F, Logger[F]] = {

    def mkDirs: F[Unit] = F.delay(Option(Paths.get(fileName).getParent).foreach(_.toFile.mkdirs()))

    def mkBuffer: F[BufferedWriter] = F.blocking(Files.newBufferedWriter(Paths.get(fileName), openOptions*))

    def closeBuffer(buffer: BufferedWriter): F[Unit] = F.blocking(buffer.close()).voidError

    Resource.make(mkDirs >> mkBuffer)(closeBuffer).map { buffer =>
      new FileLogger(buffer, formatter, minLevel)
    }
  }

}
