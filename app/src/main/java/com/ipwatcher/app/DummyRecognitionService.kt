package com.ipwatcher.app

import android.content.Intent
import android.speech.RecognitionService

/**
 * RecognitionService rỗng. Android yêu cầu mọi VoiceInteractionService phải khai báo
 * kèm một RecognitionService hợp lệ để được liệt kê trong danh sách "Trợ lý số"
 * (Settings > Apps > Default apps > Digital assistant app), nhưng app này
 * không thực hiện nhận diện giọng nói thật nên các hàm ở đây để trống.
 */
class DummyRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        // Không dùng
    }

    override fun onCancel(listener: Callback?) {
        // Không dùng
    }

    override fun onStopListening(listener: Callback?) {
        // Không dùng
    }
}
