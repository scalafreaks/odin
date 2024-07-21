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

package io.odin.json

import com.github.plokhotnyuk.jsoniter_scala.core._
import io.odin.LoggerMessage
import io.odin.formatter.{Formatter => OFormatter}
import io.odin.formatter.Formatter._
import io.odin.formatter.options.{PositionFormat, ThrowableFormat}

object Formatter {

  val json: OFormatter = create(ThrowableFormat.Default, PositionFormat.Full)

  def create(throwableFormat: ThrowableFormat): OFormatter =
    create(throwableFormat, PositionFormat.Full)

  def create(throwableFormat: ThrowableFormat, positionFormat: PositionFormat): OFormatter = { (msg: LoggerMessage) =>
    writeToString(
      Output(
        msg.level,
        msg.message.value,
        msg.context,
        msg.exception.map(t => formatThrowable(t, throwableFormat)),
        formatPosition(msg.position, positionFormat),
        msg.threadName,
        formatTimestamp(msg.timestamp)
      )
    )
  }
}
