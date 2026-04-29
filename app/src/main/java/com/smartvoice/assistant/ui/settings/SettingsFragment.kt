package com.smartvoice.assistant.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.smartvoice.assistant.data.model.Language
import com.smartvoice.assistant.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

/**
 * Settings fragment for configuring the voice assistant.
 *
 * Allows the user to:
 * - Select preferred language
 * - Toggle auto-detect language mode
 * - Enable/disable gesture recognition
 * - Toggle offline mode
 * - Toggle continuous listening mode
 * - Adjust speech rate
 * - Open accessibility settings
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLanguageSpinner()
        setupToggles()
        observeViewModel()
    }

    private fun setupLanguageSpinner() {
        val languages = Language.entries.map { "${it.displayName} (${it.nativeName})" }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            languages
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSettingsLanguage.adapter = adapter

        binding.spinnerSettingsLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.setLanguage(Language.entries[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupToggles() {
        binding.switchSettingsAutoDetect.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoDetect(isChecked)
        }

        binding.switchSettingsGesture.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setGestureEnabled(isChecked)
        }

        binding.switchSettingsOffline.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setOfflineMode(isChecked)
        }

        binding.switchSettingsContinuous.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setContinuousListening(isChecked)
        }

        binding.sliderSpeechRate.addOnChangeListener { _, value, _ ->
            viewModel.setSpeechRate(value)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.language.collect { lang ->
                        binding.spinnerSettingsLanguage.setSelection(lang.ordinal)
                    }
                }
                launch {
                    viewModel.autoDetect.collect { enabled ->
                        binding.switchSettingsAutoDetect.isChecked = enabled
                    }
                }
                launch {
                    viewModel.gestureEnabled.collect { enabled ->
                        binding.switchSettingsGesture.isChecked = enabled
                    }
                }
                launch {
                    viewModel.offlineMode.collect { enabled ->
                        binding.switchSettingsOffline.isChecked = enabled
                    }
                }
                launch {
                    viewModel.continuousListening.collect { enabled ->
                        binding.switchSettingsContinuous.isChecked = enabled
                    }
                }
                launch {
                    viewModel.speechRate.collect { rate ->
                        binding.sliderSpeechRate.value = rate
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
