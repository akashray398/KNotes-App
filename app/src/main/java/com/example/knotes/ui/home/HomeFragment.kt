package com.example.knotes.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.knotes.R
import com.example.knotes.data.repository.SyncRepository
import com.example.knotes.domain.usecase.ExportDataUseCase
import com.example.knotes.domain.usecase.ImportDataUseCase
import com.example.knotes.util.SettingsManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var syncRepository: SyncRepository

    @Inject
    lateinit var exportDataUseCase: ExportDataUseCase

    @Inject
    lateinit var importDataUseCase: ImportDataUseCase

    @Inject
    @JvmField
    var auth: FirebaseAuth? = null

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val outputStream = requireContext().contentResolver.openOutputStream(it)
                    if (outputStream != null) {
                        exportDataUseCase(outputStream).onSuccess {
                            Toast.makeText(requireContext(), "Data exported successfully", Toast.LENGTH_SHORT).show()
                        }.onFailure { e ->
                            Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(it)
                    if (inputStream != null) {
                        importDataUseCase(inputStream).onSuccess {
                            Toast.makeText(requireContext(), "Data imported successfully", Toast.LENGTH_SHORT).show()
                        }.onFailure { e ->
                            Toast.makeText(requireContext(), "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Import failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by viewModel.homeState.collectAsState()
                val themeMode by settingsManager.themeMode.collectAsState(initial = 0)
                val isAiEnabled by settingsManager.isAiEnabled.collectAsState(initial = false)
                val aiConsentGiven by settingsManager.aiConsentGiven.collectAsState(initial = false)

                HomeScreenCompose(
                    state = state,
                    currentThemeMode = themeMode,
                    isAiEnabled = isAiEnabled,
                    aiConsentGiven = aiConsentGiven,
                    isCloudSyncActive = auth?.currentUser != null,
                    onSetThemeMode = { mode ->
                        lifecycleScope.launch {
                            settingsManager.setThemeMode(mode)
                            val nightMode = when (mode) {
                                1 -> AppCompatDelegate.MODE_NIGHT_NO
                                2 -> AppCompatDelegate.MODE_NIGHT_YES
                                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            }
                            AppCompatDelegate.setDefaultNightMode(nightMode)
                        }
                    },
                    onToggleAiEnabled = { enabled ->
                        lifecycleScope.launch {
                            settingsManager.setAiEnabled(enabled)
                        }
                    },
                    onTriggerSync = {
                        syncRepository.startSync()
                        Toast.makeText(requireContext(), "Cloud Sync initiated", Toast.LENGTH_SHORT).show()
                    },
                    onToggleTaskCompletion = { task ->
                        viewModel.toggleTaskCompletion(task)
                    },
                    onNavigateToNotes = {
                        findNavController().navigate(R.id.notesFragment)
                    },
                    onNavigateToTasks = {
                        findNavController().navigate(R.id.tasksFragment)
                    },
                    onNavigateToCreateTask = {
                        val action = HomeFragmentDirections.actionHomeFragmentToEditTaskFragment(-1)
                        findNavController().navigate(action)
                    },
                    onNavigateToEditTask = { taskId ->
                        val action = HomeFragmentDirections.actionHomeFragmentToEditTaskFragment(taskId)
                        findNavController().navigate(action)
                    },
                    onNavigateToSettings = {
                        findNavController().navigate(R.id.settingsFragment)
                    },
                    onNavigateToArchive = {
                        findNavController().navigate(R.id.archiveFragment)
                    },
                    onNavigateToTrash = {
                        findNavController().navigate(R.id.trashFragment)
                    },
                    onNavigateToFolders = {
                        findNavController().navigate(R.id.foldersFragment)
                    },
                    onNavigateToAiChat = {
                        findNavController().navigate(R.id.aiChatFragment)
                    },
                    onExportData = {
                        exportLauncher.launch("knotes_backup_${System.currentTimeMillis()}.json")
                    },
                    onImportData = {
                        importLauncher.launch(arrayOf("application/json"))
                    }
                )
            }
        }
    }
}
