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

import io.odin.{LoggerMessage, OdinSpec}
import io.odin.syntax.*

import cats.data.Writer
import cats.effect.Clock
import cats.Id

class SecretLoggerSpec extends OdinSpec {

  type F[A] = Writer[List[LoggerMessage], A]

  implicit val clock: Clock[Id] = zeroClock
  implicit val clockT: Clock[F] = zeroClock

  checkAll(
    "SecretLogger",
    LoggerTests[F](new WriterTLogger[Id].withSecretContext("foo"), _.written).all
  )

  it should "modify context by hashing secret keys of a message" in {
    forAll { (msg: LoggerMessage) =>
      whenever(msg.context.nonEmpty) {
        val keys           = msg.context.keys.toList
        val logger         = new WriterTLogger[Id].withSecretContext(keys.head, keys.tail*)
        val written :: Nil = logger.log(msg).written: @unchecked
        checkHashedResult(msg, written)
      }
    }
  }

  it should "modify context by hashing secret keys of messages" in {
    forAll { (msgs: List[LoggerMessage]) =>
      val keys = msgs.flatMap(_.context.keys)
      whenever(keys.nonEmpty) {
        val msgsWithContext = msgs.filter(_.context.nonEmpty)
        val logger          = new WriterTLogger[Id].withSecretContext(keys.head, keys.tail*)
        val written         = logger.log(msgsWithContext).written
        msgsWithContext.zip(written).map {
          case (origin, result) =>
            checkHashedResult(origin, result)
        }
      }
    }
  }

  def checkHashedResult(origin: LoggerMessage, written: LoggerMessage): Unit = {
    origin.context.keys should contain theSameElementsAs written.context.keys
    origin.context.values shouldNot contain theSameElementsAs written.context.values
    all(written.context.values) should startWith("secret:")
    ()
  }

}
