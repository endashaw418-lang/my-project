package com.smartvoice.assistant.ui.main

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartvoice.assistant.R
import com.smartvoice.assistant.data.model.Language
import com.smartvoice.assistant.databinding.ActivityMainBinding
import com.smartvoice.assistant.service.speech.SpeechRecognitionService
import com.smartvoice.assistant.ui.gesture.GestureCameraFragment
import com.smartvoice.assistant.ui.history.HistoryAdapter
import com.smartvoice.assistant.ui.settings.SettingsFragment
import com.smartvoice.assistant.util.Constants
import com.smartvoice.assistant.util.PermissionManager
import kotlinx.coroutines.launch

/**
 * Main activity and entry point for the Smart Voice Assistant.
 *
 * Features:
 * - Voice activation button with animated feedback
 * - Real-time speech recognition status
 * - Language selector with auto-detect option
 * - Command history list
 * - Gesture camera toggle
 * - Settings navigation
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var permissionManager: PermissionManager
    private lateinit var historyAdapter: HistoryAdapter
    private var isFragmentShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        permissionManager = PermissionManager(this)

        setupUI()
        setupLanguageSelector()
        setupHistoryList()
        observeViewModel()
        requestPermissions()
    }

    // ─── UI Setup ─────────────────────────────────────────────────

    private fun setupUI() {
        // Voice activation button
        binding.fabMicrophone.setOnClickListener {
            if (!permissionManager.hasPermission(android.Manifest.permission.RECORD_AUDIO)) {
                permissionManager.requestEssentialPermissions(this)
                return@setOnClickListener
            }

            val currentState = viewModel.speechService.state.value
            if (currentState is SpeechRecognitionService.RecognitionState.Listening) {
                viewModel.stopListening()
                binding.fabMicrophone.setImageResource(R.drawable.ic_mic)
                binding.pulseView.visibility = View.GONE
            } else {
                viewModel.startListening()
                binding.fabMicrophone.setImageResource(R.drawable.ic_mic_off)
                binding.pulseView.visibility = View.VISIBLE
            }
        }

        // Auto-detect toggle
        binding.switchAutoDetect.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoDetect(isChecked)
            binding.spinnerLanguage.isEnabled = !isChecked
        }

        // Clear history button
        binding.btnClearHistory.setOnClickListener {
            viewModel.clearHistory()
        }
    }

    private fun setupLanguageSelector() {
        val languages = Language.entries.map { "${it.displayName} (${it.nativeName})" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguage.adapter = adapter

        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.setLanguage(Language.entries[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupHistoryList() {
        historyAdapter = HistoryAdapter()
        binding.recyclerHistory.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyAdapter
        }
    }

    // ─── Observe ViewModel ────────────────────────────────────────

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.statusText.collect { text ->
                        binding.tvStatus.text = text
                    }
                }

                launch {
                    viewModel.lastResult.collect { result ->
                        result?.let {
                            binding.tvRecognizedText.text = it.command.rawText
                            binding.tvResponseText.text = it.message
                            binding.cardResult.visibility = View.VISIBLE

                            // Update language indicator
                            binding.tvLanguageIndicator.text = it.command.language.nativeName
                        }
                    }
                }

                launch {
                    viewModel.commandHistory.collect { history ->
                        historyAdapter.submitList(history)
                        binding.tvHistoryEmpty.visibility =
                            if (history.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.isProcessing.collect { isProcessing ->
                        binding.progressBar.visibility =
                            if (isProcessing) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.speechService.state.collect { state ->
                        when (state) {
                            is SpeechRecognitionService.RecognitionState.Listening -> {
                                binding.fabMicrophone.setImageResource(R.drawable.ic_mic_off)
                                binding.pulseView.visibility = View.VISIBLE
                            }
                            else -> {
                                binding.fabMicrophone.setImageResource(R.drawable.ic_mic)
                                binding.pulseView.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }
    }

    // ─── Fragment Navigation ──────────────────────────────────────

    private fun showFragment(fragment: androidx.fragment.app.Fragment, tag: String) {
        isFragmentShowing = true
        binding.fragmentContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    private fun hideFragment() {
        if (isFragmentShowing) {
            supportFragmentManager.popBackStack()
            binding.fragmentContainer.visibility = View.GONE
            isFragmentShowing = false
        }
    }

    // ─── Permissions ──────────────────────────────────────────────

    private fun requestPermissions() {
        if (!permissionManager.hasAllPermissions()) {
            permissionManager.requestAllPermissions(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            if (!permissionManager.hasEssentialPermissions()) {
                binding.tvStatus.text = getString(R.string.permission_required)
            }
        }
    }

    // ─── Menu ─────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                if (isFragmentShowing) {
                    hideFragment()
                } else {
                    showFragment(SettingsFragment(), "settings")
                }
                true
            }
            R.id.action_accessibility -> {
                permissionManager.openAccessibilitySettings(this)
                true
            }
            R.id.action_gesture_camera -> {
                val enabled = !viewModel.gestureEnabled.value
                viewModel.setGestureEnabled(enabled)
                if (enabled) {
                    if (!permissionManager.hasPermission(android.Manifest.permission.CAMERA)) {
                        permissionManager.requestAllPermissions(this)
                        return true
                    }
                    showFragment(GestureCameraFragment(), "gesture_camera")
                } else {
                    hideFragment()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isFragmentShowing) {
            hideFragment()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        viewModel.speechService.destroy()
        viewModel.ttsService.destroy()
    }
}
