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

import java.security.MessageDigest

import io.odin.util.Hex
import io.odin.LoggerMessage

object SecretLogger {

  def apply(secrets: Set[String], algo: String = "SHA-1")(msg: LoggerMessage): LoggerMessage = {
    val md = MessageDigest.getInstance(algo)
    msg.copy(
      context = msg.context.map {
        case (key, value) =>
          if (secrets.contains(key)) {
            key -> s"secret:${Hex.encodeHex(md.digest(value.getBytes)).take(6)}"
          } else {
            key -> value
          }
      }
    )
  }

}
