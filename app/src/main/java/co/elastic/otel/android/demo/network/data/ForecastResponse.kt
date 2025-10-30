package co.elastic.otel.android.demo.network.data

import com.google.gson.annotations.SerializedName

data class ForecastResponse(
    @SerializedName("current_weather")
    val currentWeather: CurrentWeatherResponse
)