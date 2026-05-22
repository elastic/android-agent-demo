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
package co.elastic.otel.android.demo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val SkyBlue500 = Color(0xFF2196F3)
private val SkyBlue100 = Color(0xFFE3F2FD)
private val SkyBlue300 = Color(0xFF90CAF9)
private val SkyBlue700 = Color(0xFF1565C0)
private val SkyBlue900 = Color(0xFF0D47A1)
private val SunAmber700 = Color(0xFFF57F17)
private val SunAmber400 = Color(0xFFFFCA28)
private val SunAmber200 = Color(0xFFFFECB3)

private val LightColorScheme =
    lightColorScheme(
        primary = SkyBlue500,
        onPrimary = Color.White,
        primaryContainer = SkyBlue100,
        onPrimaryContainer = SkyBlue900,
        secondary = SunAmber700,
        onSecondary = Color.White,
        secondaryContainer = SunAmber200,
        onSecondaryContainer = Color.Black,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = SkyBlue300,
        onPrimary = SkyBlue900,
        primaryContainer = SkyBlue700,
        onPrimaryContainer = SkyBlue100,
        secondary = SunAmber400,
        onSecondary = Color.Black,
        secondaryContainer = SunAmber700,
        onSecondaryContainer = Color.White,
    )

@Composable
fun DemoWeatherAppTheme(content: @Composable () -> Unit) {
  val context = LocalContext.current
  val darkTheme = isSystemInDarkTheme()
  val colorScheme =
      when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
      }

  MaterialTheme(colorScheme = colorScheme, content = content)
}
