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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.elastic.otel.android.demo.network.WeatherRestManager
import co.elastic.otel.android.demo.ui.theme.DemoWeatherAppTheme

@Composable
fun ForecastScreen(city: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
  var uiState by remember { mutableStateOf<ForecastUiState>(ForecastUiState.Loading) }

  LaunchedEffect(city) {
    uiState = ForecastUiState.Loading
    uiState =
        try {
          val response = WeatherRestManager.getCurrentCityWeather(city)
          ForecastUiState.Success(response.currentWeather.temperature)
        } catch (e: Exception) {
          ForecastUiState.Error(city)
        }
  }

  Box(modifier = modifier.fillMaxSize()) {
    when (val state = uiState) {
      ForecastUiState.Loading -> LoadingContent(city)
      is ForecastUiState.Success -> SuccessContent(state.temperatureC)
      is ForecastUiState.Error -> ErrorContent(city = state.city, onBack = onBack)
    }
  }
}

@Composable
private fun LoadingContent(city: String) {
  Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    CircularProgressIndicator()
    Spacer(Modifier.height(16.dp))
    Text(
        text = "Fetching forecast for $city…",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun SuccessContent(temperatureC: Double) {
  val (startColor, endColor) = gradientColors(temperatureC)
  Box(
      modifier =
          Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(startColor, endColor))),
      contentAlignment = Alignment.Center,
  ) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp),
    ) {
      Icon(
          imageVector = weatherIcon(temperatureC),
          contentDescription = null,
          modifier = Modifier.size(96.dp),
          tint = Color.White,
      )
      Spacer(Modifier.height(16.dp))
      Text(
          text = "%.1f °C".format(temperatureC),
          style = MaterialTheme.typography.displayLarge,
          color = Color.White,
      )
      Spacer(Modifier.height(8.dp))
      Text(
          text = temperatureDescriptor(temperatureC),
          style = MaterialTheme.typography.titleMedium,
          color = Color.White.copy(alpha = 0.9f),
      )
      Spacer(Modifier.height(32.dp))
      val uriHandler = LocalUriHandler.current
      TextButton(onClick = { uriHandler.openUri("https://open-meteo.com/") }) {
        Text(
            "Weather data by Open-Meteo.com",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
        )
      }
    }
  }
}

@Composable
private fun ErrorContent(city: String, onBack: () -> Unit) {
  Column(
      modifier = Modifier.fillMaxSize().padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Icon(
        imageVector = Icons.Outlined.ErrorOutline,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.error,
    )
    Spacer(Modifier.height(16.dp))
    Text(
        text = "Couldn't load forecast for $city",
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "This city isn't supported by the local backend. Try Berlin, London, or Paris.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = onBack) { Text("Try another city") }
  }
}

private fun weatherIcon(temp: Double): ImageVector =
    when {
      temp >= 20 -> Icons.Outlined.WbSunny
      temp >= 5 -> Icons.Outlined.Cloud
      else -> Icons.Outlined.AcUnit
    }

private fun temperatureDescriptor(temp: Double): String =
    when {
      temp >= 30 -> "Hot"
      temp >= 25 -> "Warm"
      temp >= 15 -> "Mild"
      temp >= 5 -> "Cool"
      temp >= 0 -> "Chilly"
      else -> "Freezing"
    }

private fun gradientColors(temp: Double): Pair<Color, Color> =
    when {
      temp >= 25 -> Color(0xFFE65100) to Color(0xFFFFE0B2) // warm orange
      temp >= 10 -> Color(0xFF2E7D32) to Color(0xFFC8E6C9) // mild green
      temp >= 0 -> Color(0xFF1565C0) to Color(0xFFBBDEFB) // cool blue
      else -> Color(0xFF4527A0) to Color(0xFFEDE7F6) // freezing indigo
    }

@Preview(showBackground = true)
@Composable
private fun LoadingPreview() {
  DemoWeatherAppTheme { LoadingContent("Berlin") }
}

@Preview(showBackground = true)
@Composable
private fun SuccessWarmPreview() {
  DemoWeatherAppTheme { SuccessContent(28.0) }
}

@Preview(showBackground = true)
@Composable
private fun SuccessMildPreview() {
  DemoWeatherAppTheme { SuccessContent(15.0) }
}

@Preview(showBackground = true)
@Composable
private fun SuccessColdPreview() {
  DemoWeatherAppTheme { SuccessContent(-5.0) }
}

@Preview(showBackground = true)
@Composable
private fun ErrorPreview() {
  DemoWeatherAppTheme { ErrorContent(city = "New York", onBack = {}) }
}
