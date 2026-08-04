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
package co.elastic.otel.android.demo.ui.forecast

import org.junit.Assert.assertEquals
import org.junit.Test

class TemperatureDescriptorTest {
  @Test
  fun `maps temperature boundaries to descriptions`() {
    val cases =
        listOf(
            -1.0 to "Freezing",
            0.0 to "Chilly",
            4.9 to "Chilly",
            5.0 to "Cool",
            14.9 to "Cool",
            15.0 to "Mild",
            24.9 to "Mild",
            25.0 to "Warm",
            29.9 to "Warm",
            30.0 to "Hot",
        )

    cases.forEach { (temperature, expected) ->
      assertEquals(expected, temperatureDescriptor(temperature))
    }
  }
}
