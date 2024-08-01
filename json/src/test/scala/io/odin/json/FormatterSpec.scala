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

import java.time.Instant

import io.odin.meta.Position
import io.odin.Level
import io.odin.LoggerMessage
import io.odin.OdinSpec

import cats.Eval

class FormatterSpec extends OdinSpec {

  "json.format" should "generate correct json" in {
    val jsonString = Formatter.json.format(
      LoggerMessage(
        Level.Info,
        Eval.later("just a test"),
        Map("a" -> "field"),
        Some(new Exception("test exception")),
        Position.derivePosition,
        "test-thread-1",
        Instant.EPOCH.toEpochMilli
      )
    )

    // can't be bothered to pull in a proper json library to decode this and the timestamp
    // changes depending on environment and thus a bit weird way of checking the json
    jsonString should include(""""level":"INFO"""")
    jsonString should include(""""message":"just a test"""")
    jsonString should include(""""context":{"a":"field"}""")
    jsonString should include(""""exception":"Caused by: java.lang.Exception: test exception""")
    jsonString should include(""""position":"io.odin.json.FormatterSpec#jsonString:35"""")
    jsonString should include(""""thread_name":"test-thread-1"""")
    jsonString should include(""""timestamp":"1970-01-01""")
  }

  it should "serialize any LoggerMessage" in {
    forAll(loggerMessageGen) { m =>
      noException should be thrownBy Formatter.json.format(m)
    }
  }

}
