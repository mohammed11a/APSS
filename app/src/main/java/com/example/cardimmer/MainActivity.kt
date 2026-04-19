package com.example.cardimmer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
        val hours = (0..23).map { String.format("%02d:00", it) }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hours)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        val spinnerStart = findViewById<android.widget.Spinner>(R.id.spinnerStartHour)
        val spinnerEnd = findViewById<android.widget.Spinner>(R.id.spinnerEndHour)
        
        spinnerStart.adapter = adapter
        spinnerEnd.adapter = adapter

        val layoutSchedule = findViewById<View>(R.id.layoutSchedule)
        val switchSchedule = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchSchedule)
        
        switchSchedule.setOnCheckedChangeListener { _, isChecked ->
            prefs.isScheduleEnabled = isChecked
            layoutSchedule.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        spinnerStart.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.scheduleStartHour = position
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        spinnerEnd.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.scheduleEndHour = position
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val switchLock = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchLock)
        switchLock.setOnCheckedChangeListener { _, isChecked ->
            prefs.isLocked = isChecked
        }

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

        // Add a button dynamically or handle a secret tap on the title to open AutoStart
        // Since we don't have the button in XML right now, let's attach it to the root title
        // or we can just try launching it directly when checking battery optimization
    }

    private fun requestAutoStartPermission() {
        val manufacturer = Build.MANUFACTURER.lowercase()
        try {
            val intent = Intent()
            when (manufacturer) {
                "xiaomi" -> intent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                "oppo" -> intent.component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                "vivo" -> intent.component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                "letv" -> intent.component = ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")
                "honor" -> intent.component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
                else -> {
                    // For generic Android car head units, sometimes standard battery settings is enough
                    return
                }
            }
            startActivity(intent)
            Toast.makeText(this, "يرجى السماح للتطبيق بالتشغيل التلقائي من هذه القائمة", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
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

        val switchSchedule = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchSchedule)
        val switchLock = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchLock)
        val spinnerStart = findViewById<android.widget.Spinner>(R.id.spinnerStartHour)
        val spinnerEnd = findViewById<android.widget.Spinner>(R.id.spinnerEndHour)
        val layoutSchedule = findViewById<View>(R.id.layoutSchedule)

        switchSchedule.isChecked = prefs.isScheduleEnabled
        layoutSchedule.visibility = if (prefs.isScheduleEnabled) View.VISIBLE else View.GONE
        switchLock.isChecked = prefs.isLocked
        
        spinnerStart.setSelection(prefs.scheduleStartHour)
        spinnerEnd.setSelection(prefs.scheduleEndHour)
    }

    private fun onSwitchChanged() {
        if (prefs.isEnabled || prefs.isMuteEnabled) {
            checkPermissionsAndStartService()
        } else {
            stopOverlayService()
        }
        notifyServiceUpdate()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = ComponentName(this, DimmerAccessibilityService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledService = ComponentName.unflattenFromString(componentNameString)
            if (enabledService != null && enabledService == expectedComponentName) {
                return true
            }
        }
        return false
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

        if (prefs.isEnabled && !isAccessibilityServiceEnabled()) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            Toast.makeText(this, R.string.accessibility_required, Toast.LENGTH_LONG).show()
            startActivity(intent)
            // Revert switch visually because service isn't active yet
            binding.switchEnable.isChecked = false
            prefs.isEnabled = false
            return
        }

        checkBatteryOptimization()
        startOverlayService()
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                    Toast.makeText(this, "يرجى السماح للتطبيق بالعمل في الخلفية لتجنب إغلاقه", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // Also trigger custom auto-start screen if available
                requestAutoStartPermission()
            }
        }
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
