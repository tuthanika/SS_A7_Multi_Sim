package com.custom.simswitcher

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.custom.simswitcher.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsManager: PreferenceManager

    private val modeValues = intArrayOf(11, 9, 2, 1)
    private val modeNames = arrayOf(
        "4G Only (LTE Only - 11)",
        "4G/3G/2G Auto (9)",
        "3G Only (WCDMA - 2)",
        "2G Only (GSM - 1)"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferenceManager(this)

        setupSpinners()
        loadSavedValues()
        setupListeners()
        checkRootStatus()
    }

    private fun setupSpinners() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modeNames)
        binding.spinnerSim1Mode.adapter = adapter
        binding.spinnerSim2Mode.adapter = adapter
    }

    private fun loadSavedValues() {
        // Find index for SIM 1 mode
        val sim1Idx = modeValues.indexOf(prefsManager.sim1Mode).coerceAtLeast(0)
        binding.spinnerSim1Mode.setSelection(sim1Idx)

        // Find index for SIM 2 mode
        val sim2Idx = modeValues.indexOf(prefsManager.sim2Mode).coerceAtLeast(1)
        binding.spinnerSim2Mode.setSelection(sim2Idx)

        binding.etSim1Time.setText(prefsManager.sim1DurationMinutes.toString())
        binding.etSim2Time.setText(prefsManager.sim2DurationMinutes.toString())

        binding.cbToggleAirplane.isChecked = prefsManager.toggleAirplaneMode
        binding.cbAutoRotation.isChecked = prefsManager.autoRotationEnabled

        if (prefsManager.useShizuku) {
            binding.rbModeShizuku.isChecked = true
        } else {
            binding.rbModeRoot.isChecked = true
        }

        updateServiceStatusUI()
    }

    private fun setupListeners() {
        binding.rgExecutionMode.setOnCheckedChangeListener { _, checkedId ->
            prefsManager.useShizuku = (checkedId == R.id.rbModeShizuku)
        }

        binding.btnReqRoot.setOnClickListener {
            Thread {
                val hasRoot = RootUtils.isRootAvailable()
                runOnUiThread {
                    if (hasRoot) {
                        Toast.makeText(this, "Đã được cấp quyền ROOT thành công!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Không nhận được quyền ROOT! Hãy mở SuperSU / Magisk cấp quyền.", Toast.LENGTH_LONG).show()
                    }
                    checkRootStatus()
                }
            }.start()
        }

        binding.btnReqShizuku.setOnClickListener {
            RootUtils.requestShizukuPermission()
            Toast.makeText(this, "Đã gửi yêu cầu cấp quyền Shizuku!", Toast.LENGTH_SHORT).show()
            checkRootStatus()
        }

        binding.btnSave.setOnClickListener {
            saveValues()
            Toast.makeText(this, "Đã lưu cài đặt!", Toast.LENGTH_SHORT).show()
        }

        binding.btnApplyNow.setOnClickListener {
            saveValues()
            Toast.makeText(this, "Đang thực thi chuyển mạng...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, SimRotatorService::class.java).apply {
                action = if (prefsManager.currentPhase == 1) SimRotatorService.ACTION_SWITCH_SIM1 else SimRotatorService.ACTION_SWITCH_SIM2
            }
            startService(intent)
        }

        binding.btnToggleService.setOnClickListener {
            saveValues()
            if (prefsManager.isServiceRunning) {
                SimRotatorService.stopService(this)
                Toast.makeText(this, "Đã dừng Service!", Toast.LENGTH_SHORT).show()
            } else {
                SimRotatorService.startService(this)
                Toast.makeText(this, "Đã khởi chạy Service!", Toast.LENGTH_SHORT).show()
            }
            updateServiceStatusUI()
        }
    }

    private fun saveValues() {
        val sim1Pos = binding.spinnerSim1Mode.selectedItemPosition
        val sim2Pos = binding.spinnerSim2Mode.selectedItemPosition

        prefsManager.sim1Mode = modeValues[sim1Pos]
        prefsManager.sim2Mode = modeValues[sim2Pos]

        val sim1TimeStr = binding.etSim1Time.text.toString()
        val sim2TimeStr = binding.etSim2Time.text.toString()

        prefsManager.sim1DurationMinutes = sim1TimeStr.toIntOrNull() ?: 15
        prefsManager.sim2DurationMinutes = sim2TimeStr.toIntOrNull() ?: 3

        prefsManager.toggleAirplaneMode = binding.cbToggleAirplane.isChecked
        prefsManager.autoRotationEnabled = binding.cbAutoRotation.isChecked
        prefsManager.useShizuku = binding.rbModeShizuku.isChecked
    }

    private fun updateServiceStatusUI() {
        if (prefsManager.isServiceRunning) {
            binding.tvStatus.text = "Trạng thái Service: ĐANG CHẠY 🟢"
            binding.btnToggleService.text = "Dừng Service"
        } else {
            binding.tvStatus.text = "Trạng thái Service: ĐÃ DỪNG 🔴"
            binding.btnToggleService.text = "Khởi chạy Service"
        }
    }

    private fun checkRootStatus() {
        Thread {
            val hasRoot = RootUtils.isRootAvailable()
            val hasShizuku = RootUtils.isShizukuAvailable()
            runOnUiThread {
                when {
                    hasRoot && !prefsManager.useShizuku -> binding.tvRootStatus.text = "Quyền thực thi: ROOT (SuperSU / Magisk) 🟢"
                    hasShizuku && prefsManager.useShizuku -> binding.tvRootStatus.text = "Quyền thực thi: SHIZUKU 🟢"
                    hasRoot -> binding.tvRootStatus.text = "Quyền thực thi: ROOT (SuperSU / Magisk) 🟢"
                    hasShizuku -> binding.tvRootStatus.text = "Quyền thực thi: SHIZUKU 🟢"
                    else -> binding.tvRootStatus.text = "Quyền thực thi: CHƯA CẤP (Cần Root hoặc Shizuku!) 🔴"
                }
            }
        }.start()
    }
}
