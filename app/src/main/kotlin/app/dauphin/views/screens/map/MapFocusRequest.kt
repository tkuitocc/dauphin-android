package app.dauphin.views.screens.map

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 其他畫面要求「切到地圖分頁並顯示某間教室所在的建築」。
 * App 收到後切換分頁，地圖頁處理完就呼叫 [consume] 清掉。
 */
object MapFocusRequest {
    private val _room = MutableStateFlow<String?>(null)
    val room: StateFlow<String?> = _room.asStateFlow()

    fun request(roomCode: String) {
        _room.value = roomCode
    }

    fun consume() {
        _room.value = null
    }
}
