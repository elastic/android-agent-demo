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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import co.elastic.otel.android.demo.R
import co.elastic.otel.android.demo.databinding.FragmentFirstBinding

class FirstFragment : Fragment() {

  private var _binding: FragmentFirstBinding? = null
  private val binding
    get() = _binding!!

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    _binding = FragmentFirstBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.buttonFirst.setOnClickListener {
      val bundle = bundleOf("city" to binding.citySpinner.selectedItem.toString())
      findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment, bundle)
    }

    ArrayAdapter.createFromResource(
            view.context,
            R.array.city_array,
            android.R.layout.simple_spinner_item,
        )
        .also { adapter ->
          adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
          binding.citySpinner.adapter = adapter
        }
  }

  private fun doCrash() {
    throw RuntimeException("Some crash")
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
