package app.dauphin.viewmodels.other

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import app.dauphin.data.CalendarEvent
import app.dauphin.data.CampusRepository
import java.util.Calendar

sealed interface CalendarUiState {
    data object Loading : CalendarUiState
    data class Loaded(val events: List<CalendarEvent>) : CalendarUiState
    data object Error : CalendarUiState
}

class CalendarScreenViewModel(
    private val campusRepository: CampusRepository
) : ViewModel() {

    /** 8 月到隔年 1 月屬於上學期，其餘為下學期。 */
    private val _term = MutableStateFlow(
        if (Calendar.getInstance().get(Calendar.MONTH) + 1 in listOf(8, 9, 10, 11, 12, 1)) 1 else 2
    )
    val term: StateFlow<Int> = _term.asStateFlow()

    private val _uiState = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun setTerm(term: Int) {
        if (term == _term.value) return
        _term.value = term
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = CalendarUiState.Loading
            _uiState.value = try {
                CalendarUiState.Loaded(campusRepository.fetchCalendar(_term.value))
            } catch (e: Exception) {
                CalendarUiState.Error
            }
        }
    }
}
