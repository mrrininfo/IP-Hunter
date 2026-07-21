package com.ipwatcher.app

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ipwatcher.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                "Chưa cấp quyền thông báo - bạn sẽ không thấy được trạng thái theo dõi IP. Vào Settings > Apps > IP Watcher > Notifications để cấp.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        binding.editPrefix.setText(Prefs.getDesiredPrefix(this))
        binding.editInterval.setText(Prefs.getCheckIntervalSec(this).toString())

        binding.btnSave.setOnClickListener {
            Prefs.setDesiredPrefix(this, binding.editPrefix.text.toString().trim())
            val sec = binding.editInterval.text.toString().toIntOrNull() ?: 5
            Prefs.setCheckIntervalSec(this, sec)
            Toast.makeText(this, "Đã lưu cấu hình", Toast.LENGTH_SHORT).show()
        }

        binding.btnSetAssistant.setOnClickListener { requestAssistantRole() }

        binding.btnTestToggle.setOnClickListener { testToggle() }

        binding.btnStart.setOnClickListener {
            val intent = Intent(this, IpCheckService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "Đã bắt đầu theo dõi IP", Toast.LENGTH_SHORT).show()
        }

        binding.btnStop.setOnClickListener {
            val intent = Intent(this, IpCheckService::class.java).apply {
                action = IpCheckService.ACTION_STOP
            }
            startService(intent)
            Toast.makeText(this, "Đã dừng", Toast.LENGTH_SHORT).show()
        }

        updateAssistantStatusLabel()
    }

    override fun onResume() {
        super.onResume()
        updateAssistantStatusLabel()
    }

    private fun testToggle() {
        val sentOn = IpVoiceInteractionService.requestAirplaneToggle(this, true)
        if (!sentOn) {
            Toast.makeText(
                this,
                "Gửi lệnh THẤT BẠI ngay từ bước đầu - có thể app chưa thực sự được hệ thống nhận là Trợ lý (thử khởi động lại máy sau khi đặt Trợ lý).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        Toast.makeText(this, "Đã gửi lệnh BẬT Airplane Mode - kiểm tra thanh trạng thái ngay", Toast.LENGTH_LONG).show()

        android.os.Handler(mainLooper).postDelayed({
            val sentOff = IpVoiceInteractionService.requestAirplaneToggle(this, false)
            Toast.makeText(
                this,
                if (sentOff) "Đã gửi lệnh TẮT Airplane Mode" else "Gửi lệnh TẮT thất bại",
                Toast.LENGTH_LONG
            ).show()
        }, 3000L)
    }

    private fun updateAssistantStatusLabel() {
        val isAssistant = IpVoiceInteractionService.isCurrentlyActiveAssistant(this)
        binding.tvAssistantStatus.text = if (isAssistant) {
            "Trạng thái: Đã là Trợ lý mặc định ✅ (app có thể tự toggle Airplane Mode)"
        } else {
            "Trạng thái: CHƯA là Trợ lý mặc định ⚠️ – nhấn nút dưới để cấp quyền"
        }
    }

    private fun requestAssistantRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                    Toast.makeText(this, "App đã là Trợ lý mặc định ✅", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)
                    startActivity(intent)
                }
                return
            }
        }
        // Thiết bị cũ hơn hoặc không hỗ trợ RoleManager cho ROLE_ASSISTANT:
        // mở trực tiếp màn hình chọn Trợ lý giọng nói trong Settings.
        try {
            startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Vào Settings > Apps > Default apps > Digital assistant app và chọn IP Watcher",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
