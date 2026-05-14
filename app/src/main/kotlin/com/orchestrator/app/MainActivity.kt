package com.orchestrator.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.orchestrator.app.data.repository.AuthRepository
import com.orchestrator.app.nav.AppNavGraph
import com.orchestrator.app.ui.theme.OrchestratorTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrchestratorTheme {
                AppNavGraph(authRepository = authRepository)
            }
        }
    }
}
