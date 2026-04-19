package com.example.cardimmer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.cardimmer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager

    private val overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(this)) {
            checkPermissionsAndStartService()
        } else {
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show()
            binding.switchEnable.isChecked = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)

        setupUI()
        loadSettings()
    }

    private fun setupUI() {
        binding.switchEnable.setOnCheckedChangeListener { _, isChecked ->
            prefs.isEnabled = isChecked
            onSwitchChanged()
        }

        binding.switchMute.setOnCheckedChangeListener { _, isChecked ->
            prefs.isMuteEnabled = isChecked
            onSwitchChanged()
        }

        binding.seekDimLevel.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    prefs.dimLevel = progress / 100f
                    notifyServiceUpdate()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.spinnerButtonSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (prefs.buttonSize != position) {
                    prefs.buttonSize = position
                    notifyServiceUpdate()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.seekButtonOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    prefs.buttonOpacity = progress / 100f
                    notifyServiceUpdate()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.checkAutoHide.setOnCheckedChangeListener { _, isChecked ->
            prefs.autoHide = isChecked
            notifyServiceUpdate()
        }

        binding.checkAutoStart.setOnCheckedChangeListener { _, isChecked ->
            prefs.autoStart = isChecked
        }

        binding.btnReset.setOnClickListener {
            prefs.resetToDefaults()
            loadSettings()
            notifyServiceUpdate()
            if (!prefs.isEnabled) {
                stopOverlayService()
            }
        }
    }

    private fun loadSettings() {
        binding.switchEnable.isChecked = prefs.isEnabled
        binding.switchMute.isChecked = prefs.isMuteEnabled
        binding.seekDimLevel.progress = (prefs.dimLevel * 100).toInt()
        binding.spinnerButtonSize.setSelection(prefs.buttonSize)
        binding.seekButtonOpacity.progress = (prefs.buttonOpacity * 100).toInt()
        binding.checkAutoHide.isChecked = prefs.autoHide
        binding.checkAutoStart.isChecked = prefs.autoStart
    }

    private fun onSwitchChanged() {
        if (prefs.isEnabled || prefs.isMuteEnabled) {
            checkPermissionsAndStartService()
        } else {
            stopOverlayService()
        }
        notifyServiceUpdate()
    }

    private fun checkPermissionsAndStartService() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            Toast.makeText(this, R.string.draw_over_apps_reason, Toast.LENGTH_LONG).show()
            overlayPermissionLauncher.launch(intent)
            return
        }

        startOverlayService()
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        stopService(intent)
    }

    private fun notifyServiceUpdate() {
        if ((prefs.isEnabled || prefs.isMuteEnabled) && Settings.canDrawOverlays(this)) {
            val intent = Intent(this, OverlayService::class.java).apply {
                action = OverlayService.ACTION_UPDATE_SETTINGS
            }
            startService(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Sync UI state in case service was stopped from notification
        binding.switchEnable.isChecked = prefs.isEnabled
        // Update dim level slider if it was changed by drag
        binding.seekDimLevel.progress = (prefs.dimLevel * 100).toInt()
    }
}
