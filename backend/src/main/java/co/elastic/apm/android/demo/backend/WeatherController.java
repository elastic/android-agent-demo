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
package co.elastic.apm.android.demo.backend;

import co.elastic.apm.android.demo.backend.data.ForecastResponse;
import co.elastic.apm.android.demo.backend.data.Location;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping(path = "/v1")
public class WeatherController {

  private static final String WEATHER_SERVICE_URL = "https://api.open-meteo.com/v1/";
  RestTemplate weatherClient = new RestTemplate();

  @GetMapping(path = "/forecast", produces = "application/json")
  public ForecastResponse getWeatherForecast(@RequestParam() String city) {
    Location location = cityToGeoLocation(city);
    String url =
        WEATHER_SERVICE_URL
            + "forecast?current_weather=true&latitude="
            + location.getLatitude()
            + "&longitude="
            + location.getLongitude();
    return weatherClient.getForEntity(url, ForecastResponse.class).getBody();
  }

  private Location cityToGeoLocation(String city) {
    switch (city) {
      case "Berlin":
        return new Location(52.5167, 13.3833);
      case "London":
        return new Location(51.5072, -0.1275);
      case "Paris":
        return new Location(48.8566, 2.3522);
      default:
        throw new IllegalArgumentException(
            "This service can only retrieve geo locations for European cities!");
    }
  }
}
