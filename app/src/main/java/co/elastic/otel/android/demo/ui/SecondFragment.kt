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
package co.elastic.otel.android.demo.ui

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import co.elastic.otel.android.demo.R
import co.elastic.otel.android.demo.databinding.FragmentSecondBinding
import co.elastic.otel.android.demo.network.WeatherRestManager
import co.elastic.otel.android.demo.network.data.ForecastResponse
import kotlinx.coroutines.launch

class SecondFragment : Fragment() {

  private var _binding: FragmentSecondBinding? = null
  private val binding
    get() = _binding!!

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    _binding = FragmentSecondBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    lifecycleScope.launch {
      try {
        val city = arguments?.getString("city") ?: "Berlin"
        binding.temperatureTitle.text = getString(R.string.temperature_title, city)
        updateTemperature(WeatherRestManager.getCurrentCityWeather(city))
        showApiNotice()
      } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(requireContext(), R.string.unknown_error_message, Toast.LENGTH_SHORT).show()
      }
    }

    binding.buttonSecond.setOnClickListener {
      findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
    }
  }

  private fun showApiNotice() {
    binding.txtApiNotice.movementMethod = LinkMovementMethod.getInstance()
    binding.txtApiNotice.text =
        HtmlCompat.fromHtml(
            getString(R.string.weather_api_notice_message),
            HtmlCompat.FROM_HTML_MODE_LEGACY,
        )
  }

  private fun updateTemperature(response: ForecastResponse) {
    binding.txtDegreesCelsius.text =
        getString(R.string.temperature_in_celsius, response.currentWeather.temperature)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
