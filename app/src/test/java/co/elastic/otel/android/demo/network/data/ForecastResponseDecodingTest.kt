/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package co.elastic.otel.android.demo.network.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastResponseDecodingTest {
  @Test
  fun `decodes current weather temperature`() {
    val response =
        Gson()
            .fromJson(
                """{"current_weather":{"temperature":12.5}}""",
                ForecastResponse::class.java,
            )

    assertEquals(12.5, response.currentWeather.temperature, 0.0)
  }
}
