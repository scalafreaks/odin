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

object Output {

  private[json] final case class Default(
      level: Level,
      message: String,
      context: Map[String, String],
      exception: Option[String],
      position: String,
      threadName: String,
      timestamp: String
  )

  /**
    * Elastic Common Schema fields
    *
    * @see [[https://www.elastic.co/guide/en/ecs/current/ecs-guidelines.html ECS Guidelines]]
    * @see [[https://www.elastic.co/guide/en/ecs/current/ecs-field-reference.html ECS Field Reference]]
    */
  private[json] final case class ECS(
      `@timestamp`: String,
      message: String,
      labels: Map[String, String],
      `log.level`: Level,
      `log.logger`: Option[String],
      `log.origin.file.line`: Int,
      `log.origin.file.name`: String,
      `log.origin.function`: Option[String],
      `process.thread.name`: String,
      `error.stack_trace`: Option[String]
  )

  /**
    * Logstash fields (as defined by `logstash-logback-encoder`).
    *
    * Some fields like `level_value` are ignored for efficiency. Reach out to the maintainers if needed.
    *
    * @see [[https://github.com/logfellow/logstash-logback-encoder?tab=readme-ov-file#standard-fields Standard Fields]]
    */
  private[json] final case class Logstash(
      `@timestamp`: String,
      message: String,
      level: Level,
      logger_name: Option[String],
      thread_name: String,
      stack_trace: Option[String]
  )

  private[json] implicit val levelCodec: JsonValueCodec[Level] = new JsonValueCodec[Level] {

    // we never decode these
    override def decodeValue(in: JsonReader, default: Level): Level = ???

    override def encodeValue(x: Level, out: JsonWriter): Unit = out.writeVal(x.show)

    override def nullValue: Level = null

  }

  private[json] implicit val defaultCodec: JsonValueCodec[Default] =
    JsonCodecMaker.make(CodecMakerConfig.withFieldNameMapper(JsonCodecMaker.enforce_snake_case))

  private[json] implicit val ecsCodec: JsonValueCodec[ECS] = JsonCodecMaker.make

  private[json] implicit val logstashCodec: JsonValueCodec[Logstash] = JsonCodecMaker.make

}
