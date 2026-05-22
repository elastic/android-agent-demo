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
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import co.elastic.otel.android.demo.MyApp.Companion.agent
import co.elastic.otel.android.demo.R
import co.elastic.otel.android.demo.databinding.ActivityMainBinding
import co.elastic.otel.android.extensions.log
import co.elastic.otel.android.extensions.span
import io.opentelemetry.api.common.Attributes

class MainActivity : AppCompatActivity() {

  private lateinit var appBarConfiguration: AppBarConfiguration
  private lateinit var binding: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    agent.span("Main Activity creation") {
      binding = ActivityMainBinding.inflate(layoutInflater)
      setContentView(binding.root)

      setSupportActionBar(binding.toolbar)

      val navController = findNavController(R.id.nav_host_fragment_content_main)
      appBarConfiguration = AppBarConfiguration(navController.graph)
      setupActionBarWithNavController(navController, appBarConfiguration)

      binding.fab.setOnClickListener {
        agent.log(
          "Crash button click",
          attributes =
            Attributes.builder()
              .put("activity.name", "MainActivity")
              .put("button.action", "crash")
              .build(),
        )
        triggerDemoCrash()
      }

      val fabBaseMargin = resources.getDimensionPixelSize(R.dimen.fab_margin_bottom)
      ViewCompat.setOnApplyWindowInsetsListener(binding.fab) { view, windowInsets ->
        val insets =
          windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
          )
        view.updateLayoutParams<CoordinatorLayout.LayoutParams> {
          bottomMargin = insets.bottom + fabBaseMargin
        }
        windowInsets
      }
    }
  }

  private fun triggerDemoCrash() {
    throw RuntimeException(getString(R.string.demo_crash_message))
  }

  override fun onSupportNavigateUp(): Boolean {
    val navController = findNavController(R.id.nav_host_fragment_content_main)
    return navController.navigateUp() || super.onSupportNavigateUp()
  }
}
