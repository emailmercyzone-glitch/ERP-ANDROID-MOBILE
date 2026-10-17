package com.example.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.repository.ErpRepositoryImpl
import com.example.domain.model.Role
import com.example.presentation.cash.CashScreen
import com.example.presentation.components.OmahBottomBar
import com.example.presentation.components.OmahTopBar
import com.example.presentation.components.PinAuthDialog
import com.example.presentation.navigation.NavTab
import com.example.presentation.pos.PosScreen
import com.example.presentation.printing.PrintingScreen
import com.example.presentation.purchasing.PurchasingScreen
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.theme.OmahErpTheme
import com.example.presentation.viewmodel.AppViewModel

@Composable
fun OmahErpApp(
    repository: ErpRepositoryImpl,
    appViewModel: AppViewModel
) {
    val currentRole by appViewModel.currentRole.collectAsState()
    val currentTab by appViewModel.currentTab.collectAsState()
    val isModeRame by appViewModel.isModeRame.collectAsState()
    val themePack by appViewModel.themePack.collectAsState()
    val activeShift by appViewModel.activeShift.collectAsState()
    val storeName by appViewModel.storeName.collectAsState()
    val isModeJagaActive by appViewModel.isModeJagaActive.collectAsState()

    var showPinDialogForRole by remember { mutableStateOf<Role?>(null) }

    // Back handler returns to POS / JUAL tab if currently on sub-tabs
    BackHandler(enabled = currentTab != NavTab.JUAL) {
        appViewModel.selectTab(NavTab.JUAL)
    }

    OmahErpTheme(themePack = themePack) {
        Scaffold(
            topBar = {
                OmahTopBar(
                    storeName = storeName,
                    currentRole = currentRole,
                    isModeRame = isModeRame,
                    onToggleModeRame = { appViewModel.toggleModeRame() },
                    onOpenRoleSwitcher = {
                        val target = if (currentRole == Role.OWNER) Role.KASIR else Role.OWNER
                        showPinDialogForRole = target
                    }
                )
            },
            bottomBar = {
                OmahBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        appViewModel.selectTab(tab)
                    }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val cashierId = if (currentRole == Role.OWNER) "OWNER-01" else "KSR-01"
                val shiftId = activeShift?.shiftId ?: "SHF-K1-INIT"

                if (isModeRame) {
                    // Instant rendering without animation in Mode Rame (D-022)
                    when (currentTab) {
                        NavTab.JUAL -> PosScreen(repository, cashierId, shiftId, isModeRame)
                        NavTab.BELI -> PurchasingScreen(repository, cashierId)
                        NavTab.BAYAR -> CashScreen(repository, currentRole, isModeJagaActive, cashierId)
                        NavTab.CETAK -> PrintingScreen(repository)
                        NavTab.LAINNYA -> SettingsScreen(repository, appViewModel)
                    }
                } else {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tab_transition"
                    ) { tab ->
                        when (tab) {
                            NavTab.JUAL -> PosScreen(repository, cashierId, shiftId, isModeRame)
                            NavTab.BELI -> PurchasingScreen(repository, cashierId)
                            NavTab.BAYAR -> CashScreen(repository, currentRole, isModeJagaActive, cashierId)
                            NavTab.CETAK -> PrintingScreen(repository)
                            NavTab.LAINNYA -> SettingsScreen(repository, appViewModel)
                        }
                    }
                }
            }
        }

        // PIN Verification Dialog for role switching
        if (showPinDialogForRole != null) {
            PinAuthDialog(
                targetRole = showPinDialogForRole!!,
                onDismiss = { showPinDialogForRole = null },
                onPinVerified = { success ->
                    if (success) {
                        appViewModel.switchRole(showPinDialogForRole!!)
                    }
                    showPinDialogForRole = null
                }
            )
        }
    }
}
