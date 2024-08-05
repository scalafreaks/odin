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

import io.odin.Level

import cats.syntax.show.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

final private[json] case class Output(
    level: Level,
    message: String,
    context: Map[String, String],
    exception: Option[String],
    position: String,
    threadName: String,
    timestamp: String
)

object Output {

  implicit private[json] val levelCodec: JsonValueCodec[Level] = new JsonValueCodec[Level] {

    // we never decode these
    override def decodeValue(in: JsonReader, default: Level): Level = ???

    override def encodeValue(x: Level, out: JsonWriter): Unit = out.writeVal(x.show)

    override def nullValue: Level = null

  }

  implicit private[json] val codec: JsonValueCodec[Output] =
    JsonCodecMaker.make(CodecMakerConfig.withFieldNameMapper(JsonCodecMaker.enforce_snake_case))

}
