package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.SpeechItemRepository
import com.example.ui.TTSViewModel
import com.example.ui.TTSViewModelFactory
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.theme.MyApplicationTheme

enum class ScreenTab {
    Reader, History
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core DB extraction
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = SpeechItemRepository(database.speechItemDao)

        setContent {
            MyApplicationTheme {
                // Wrap in CompositionLocalProvider to enforce RTL Arabic layout orientation universally!
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    
                    val viewModel: TTSViewModel = viewModel(
                        factory = TTSViewModelFactory(applicationContext as Application, repository)
                    )

                    var currentTab by remember { mutableStateOf(ScreenTab.Reader) }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = stringResource(R.string.app_header),
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    titleContentColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                modifier = Modifier.testTag("app_navigation_bar"),
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp
                            ) {
                                // Reader Tab
                                NavigationBarItem(
                                    selected = currentTab == ScreenTab.Reader,
                                    onClick = { currentTab = ScreenTab.Reader },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == ScreenTab.Reader) Icons.Filled.VolumeUp else Icons.Outlined.VolumeUp,
                                            contentDescription = stringResource(R.string.tab_reader)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(R.string.tab_reader),
                                            fontWeight = if (currentTab == ScreenTab.Reader) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.testTag("tab_reader")
                                )

                                // History Tab
                                NavigationBarItem(
                                    selected = currentTab == ScreenTab.History,
                                    onClick = { currentTab = ScreenTab.History },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == ScreenTab.History) Icons.Filled.History else Icons.Outlined.History,
                                            contentDescription = stringResource(R.string.tab_history)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(R.string.tab_history),
                                            fontWeight = if (currentTab == ScreenTab.History) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.testTag("tab_history")
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                ScreenTab.Reader -> {
                                    ReaderScreen(viewModel = viewModel)
                                }
                                ScreenTab.History -> {
                                    HistoryScreen(
                                        viewModel = viewModel,
                                        onItemSelected = {
                                            // Smooth context transition: return to reader screen
                                            currentTab = ScreenTab.Reader
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
