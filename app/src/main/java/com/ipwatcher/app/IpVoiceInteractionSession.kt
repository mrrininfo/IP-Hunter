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

    // Không tạo UI hiển thị cho phiên trợ lý này
    override fun onCreateContentView(): View? = null

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

        // Không đóng session ngay lập tức - nhường thời gian cho hệ thống xử lý
        // xong Intent trước khi phiên bị huỷ. Đóng quá sớm có thể khiến lệnh
        // chưa kịp thực thi bị hủy giữa đường trên một số thiết bị.
        Handler(Looper.getMainLooper()).postDelayed({ hide() }, 1500L)
    }
}
