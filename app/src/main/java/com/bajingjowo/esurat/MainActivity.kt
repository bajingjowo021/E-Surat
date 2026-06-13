package com.bajingjowo.esurat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.bajingjowo.esurat.ui.AppNavigationScaffold
import com.bajingjowo.esurat.ui.VillageViewModel
import com.bajingjowo.esurat.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: VillageViewModel by viewModels {
        VillageViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigationScaffold(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
