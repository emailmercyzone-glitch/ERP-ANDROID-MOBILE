package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ErpRepositoryImpl
import com.example.domain.model.CashierShift
import com.example.domain.model.Role
import com.example.domain.model.ShiftStatus
import com.example.domain.model.User
import com.example.presentation.navigation.NavTab
import com.example.presentation.theme.ThemePackOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(private val repository: ErpRepositoryImpl) : ViewModel() {

    private val _currentRole = MutableStateFlow(Role.OWNER)
    val currentRole: StateFlow<Role> = _currentRole.asStateFlow()

    private val _currentTab = MutableStateFlow(NavTab.JUAL)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    private val _isModeRame = MutableStateFlow(false)
    val isModeRame: StateFlow<Boolean> = _isModeRame.asStateFlow()

    private val _themePack = MutableStateFlow(ThemePackOption.HIJAU_SEGAR)
    val themePack: StateFlow<ThemePackOption> = _themePack.asStateFlow()

    private val _isModeJagaActive = MutableStateFlow(false)
    val isModeJagaActive: StateFlow<Boolean> = _isModeJagaActive.asStateFlow()

    val activeShift: StateFlow<CashierShift?> = repository.getActiveShift("KSR-01")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val storeName: StateFlow<String> = repository.getConfigValue("STORE_NAME")
        .map { it ?: "Omah Sembako" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Omah Sembako")

    init {
        viewModelScope.launch {
            repository.openShift("KSR-01", "Siti Aminah (Kasir)", 200000L, "K1")
        }
    }

    fun selectTab(tab: NavTab) {
        _currentTab.value = tab
    }

    fun toggleModeRame() {
        _isModeRame.value = !_isModeRame.value
    }

    fun setThemePack(pack: ThemePackOption) {
        _themePack.value = pack
    }

    fun switchRole(role: Role) {
        _currentRole.value = role
    }

    fun toggleModeJaga(active: Boolean) {
        _isModeJagaActive.value = active
    }

    fun openCashierShift(startingCash: Long) {
        viewModelScope.launch {
            repository.openShift("KSR-01", "Siti Aminah (Kasir)", startingCash, "K1")
        }
    }

    fun closeCashierShift(actualCash: Long, note: String?) {
        viewModelScope.launch {
            val current = activeShift.value
            if (current != null) {
                repository.closeShift(current.shiftId, actualCash, note)
            }
        }
    }
}
