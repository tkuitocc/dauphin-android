package app.dauphin.viewmodels.other

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dauphin.data.CourseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class BarcodeScreenViewModel(
    private val courseRepository: CourseRepository
) : ViewModel() {
    val studentId: StateFlow<String?> = courseRepository.studentIdFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = null
        )
}
