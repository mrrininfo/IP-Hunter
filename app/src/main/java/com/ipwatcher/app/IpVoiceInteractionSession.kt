package com.ipwatcher.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.voice.VoiceInteractionSession
import android.util.Log
import android.view.View

class IpVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    companion object {
        const val ARG_ENABLE = "arg_enable"
        private const val TAG = "IpWatcher"
    }

    // Trả về một View thật (1x1px, trong suốt) thay vì null.
    // Một số thiết bị có thể coi session không có cửa sổ nào là "không hợp lệ"
    // và từ chối thực hiện hành động rủi ro (mất toàn bộ kết nối mạng nếu
    // không còn Wi-Fi dự phòng), kể cả khi không có gì hiển thị rõ trên màn hình.
    override fun onCreateContentView(): View {
        return View(context).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(1, 1)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        val enable = args?.getBoolean(ARG_ENABLE) ?: false
        Log.d(TAG, "Session onShow, enable=$enable")

        val intent = Intent(Settings.ACTION_VOICE_CONTROL_AIRPLANE_MODE).apply {
            putExtra(Settings.EXTRA_AIRPLANE_MODE_ENABLED, enable)
        }

        // Kiểm tra xem hệ thống có component nào xử lý được Intent này không.
        // Nếu resolveActivity trả về null, nghĩa là thiết bị/OEM này không hỗ trợ
        // Intent bật/tắt Airplane Mode qua đường Trợ lý - không có cách nào khác
        // ngoài root hoặc ADB trên máy này.
        val resolved = intent.resolveActivity(context.packageManager)
        Log.d(TAG, "resolveActivity result: $resolved")

        try {
            startVoiceActivity(intent)
            Log.d(TAG, "startVoiceActivity called successfully")
        } catch (e: Exception) {
            Log.e(TAG, "startVoiceActivity failed", e)
        }

        // Chờ lâu hơn trước khi đóng session - việc tắt sóng di động (khác Wi-Fi)
        // có thể cần nhiều thời gian xử lý hơn hoặc cần cửa sổ session tồn tại
        // lâu hơn để hệ thống coi là hợp lệ.
        Handler(Looper.getMainLooper()).postDelayed({ hide() }, 6000L)
    }
}
