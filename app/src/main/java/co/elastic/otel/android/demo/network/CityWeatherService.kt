package co.elastic.otel.android.demo.network

import co.elastic.otel.android.demo.network.data.ForecastResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface CityWeatherService {
    @GET("forecast")
    suspend fun getCurrentWeather(
        @Query("city") city: String
    ): ForecastResponse
}