package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.ErpDatabase
import com.example.data.repository.ErpRepositoryImpl
import com.example.presentation.OmahErpApp
import com.example.presentation.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = ErpDatabase.getInstance(applicationContext)
        val repository = ErpRepositoryImpl(database)

        setContent {
            val appViewModel: AppViewModel = viewModel {
                AppViewModel(repository)
            }
            OmahErpApp(
                repository = repository,
                appViewModel = appViewModel
            )
        }
    }
}
