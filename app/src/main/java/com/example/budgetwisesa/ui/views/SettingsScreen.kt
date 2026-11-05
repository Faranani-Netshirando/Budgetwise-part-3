package com.example.budgetwisesa.ui.views

import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.work.WorkInfo
import com.example.budgetwisesa.MainActivity
import com.example.budgetwisesa.R
import com.example.budgetwisesa.databinding.ScreenSettingsBinding

class SettingsScreen(private val host: MainActivity,
    private val binding: ScreenSettingsBinding = ScreenSettingsBinding.inflate(host.layoutInflater)) : NativeScreen(binding.root) {
    private var updating = false
    private var dragging = false
    init {
        refresh()
        binding.dark.setOnCheckedChangeListener { _, checked -> if (!updating) host.model.toggleDarkMode(checked) }
        binding.notifications.setOnCheckedChangeListener { _, checked -> if (!updating) host.enableNotifications(checked) }
        binding.languages.setOnCheckedChangeListener { _, id ->
            if (!updating) {
                val code = when (id) { R.id.language_ve -> "ve"; R.id.language_zu -> "zu"; else -> "en" }
                if (code != host.model.language.value) host.model.setLanguage(code)
            }
        }
        binding.threshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.thresholdLabel.text = host.getString(R.string.alert_threshold, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { dragging = true }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                dragging = false; host.model.setThreshold(binding.threshold.progress)
            }
        })
        binding.sync.setOnClickListener { host.model.syncNow() }
        binding.logout.setOnClickListener { host.model.logout() }
    }
    override fun refresh() {
        if (!active) return
        updating = true
        binding.profile.text = host.model.userName.value
        binding.dark.isChecked = host.model.isDarkMode.value
        binding.notifications.isChecked = host.model.notificationsEnabled.value
        binding.languages.check(when (host.model.language.value) { "ve" -> R.id.language_ve; "zu" -> R.id.language_zu; else -> R.id.language_en })
        if (!dragging) {
            binding.threshold.progress = host.model.alertThreshold.value
            binding.thresholdLabel.text = host.getString(R.string.alert_threshold, host.model.alertThreshold.value)
        }
        val guest = host.model.isGuest.value
        binding.syncNotice.setText(if (guest) R.string.guest_sync_notice else R.string.account_sync_notice)
        binding.pending.isVisible = !guest
        binding.status.isVisible = !guest
        binding.sync.isVisible = !guest
        binding.pending.text = host.getString(R.string.pending_changes, host.model.pendingCount.value)
        val work = host.model.syncWork.value
        binding.status.setText(when {
            work.any { it.state == WorkInfo.State.RUNNING } -> R.string.sync_running
            work.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED } -> R.string.sync_waiting
            work.lastOrNull()?.state == WorkInfo.State.FAILED -> R.string.sync_failed
            work.lastOrNull()?.state == WorkInfo.State.SUCCEEDED -> R.string.sync_complete
            else -> R.string.sync_ready
        })
        binding.sync.isEnabled = work.none { it.state == WorkInfo.State.RUNNING }
        updating = false
    }
}
