package com.ipwatcher.app

import android.content.ComponentName
import android.os.Bundle
import android.service.voice.VoiceInteractionService

/**
 * Service này chỉ có hiệu lực khi người dùng đã chọn app làm
 * "Trợ lý số mặc định" (Digital assistant app) trong Settings.
 * Khi đó, showSession() cho phép app kích hoạt một VoiceInteractionSession
 * có quyền hệ thống để gửi Intent bật/tắt Airplane Mode
 * (Settings.ACTION_VOICE_CONTROL_AIRPLANE_MODE) mà KHÔNG cần root,
 * KHÔNG cần ADB, KHÔNG cần WRITE_SECURE_SETTINGS.
 */
class IpVoiceInteractionService : VoiceInteractionService() {

    companion object {
        @Volatile
        var instance: IpVoiceInteractionService? = null

        /** true nếu app hiện đang được hệ thống chọn làm Trợ lý số mặc định */
        fun isCurrentlyActiveAssistant(context: android.content.Context): Boolean {
            return isActiveService(
                context,
                ComponentName(context, IpVoiceInteractionService::class.java)
            )
        }

        /**
         * Gọi hàm này từ bất kỳ đâu trong app để yêu cầu bật/tắt Airplane Mode.
         * Trả về false nếu app chưa được chọn làm Trợ lý mặc định (chưa có quyền).
         */
        fun requestAirplaneToggle(context: android.content.Context, enable: Boolean): Boolean {
            val isAssistant = isCurrentlyActiveAssistant(context)
            android.util.Log.d("IpWatcher", "requestAirplaneToggle: isCurrentlyActiveAssistant=$isAssistant, instance=$instance")
            if (!isAssistant) return false
            val service = instance ?: run {
                android.util.Log.w("IpWatcher", "requestAirplaneToggle: service instance is null - VoiceInteractionService chưa được hệ thống khởi tạo")
                return false
            }
            val args = Bundle()
            args.putBoolean(IpVoiceInteractionSession.ARG_ENABLE, enable)
            service.showSession(args, android.service.voice.VoiceInteractionSession.SHOW_WITH_ASSIST)
            android.util.Log.d("IpWatcher", "showSession($enable) called")
            return true
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }
}
