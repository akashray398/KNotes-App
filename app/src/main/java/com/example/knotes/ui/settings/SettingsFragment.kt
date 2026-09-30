package com.example.knotes.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.knotes.R
import com.example.knotes.data.repository.SyncRepository
import com.example.knotes.domain.usecase.ClearAllDataUseCase
import com.example.knotes.domain.usecase.ExportDataUseCase
import com.example.knotes.domain.usecase.ImportDataUseCase
import com.example.knotes.util.ConnectivityObserver
import com.example.knotes.databinding.FragmentSettingsBinding
import com.example.knotes.util.SettingsManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: com.example.knotes.ui.auth.AuthViewModel by viewModels()

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var syncRepository: SyncRepository

    @Inject
    lateinit var exportDataUseCase: ExportDataUseCase

    @Inject
    lateinit var importDataUseCase: ImportDataUseCase

    @Inject
    lateinit var clearAllDataUseCase: ClearAllDataUseCase

    @Inject
    lateinit var connectivityObserver: ConnectivityObserver

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
                    Toast.makeText(requireContext(), "Error opening output stream", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "Error opening input stream", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAuthUI()
        setupAiSettings()
        setupAppearanceSettings()
        setupDataManagement()
        observeSyncState()
        observeNetworkState()
        
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupAuthUI() {
        updateAuthVisibility()

        binding.btnLogin.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }

        binding.btnRegister.setOnClickListener {
            findNavController().navigate(R.id.signupFragment)
        }

        binding.btnForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }

        binding.btnSignOut.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log Out of KNotes?")
                .setMessage("Are you sure you want to log out? Your notes and tasks will remain safely stored on your account.")
                .setPositiveButton("Log Out") { _, _ ->
                    authViewModel.logout {
                        findNavController().navigate(R.id.loginFragment)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnSyncNow.setOnClickListener {
            syncRepository.startSync(manual = true)
        }
    }

    private fun setupAppearanceSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            val themeMode = settingsManager.themeMode.first()
            val checkedId = when (themeMode) {
                1 -> R.id.btn_theme_light
                2 -> R.id.btn_theme_dark
                else -> R.id.btn_theme_system
            }
            binding.toggleGroupTheme.check(checkedId)
            
            binding.toggleGroupTheme.addOnButtonCheckedListener { _, clickedId, isChecked ->
                if (isChecked) {
                    val mode = when (clickedId) {
                        R.id.btn_theme_light -> 1
                        R.id.btn_theme_dark -> 2
                        else -> 0
                    }
                    viewLifecycleOwner.lifecycleScope.launch {
                        settingsManager.setThemeMode(mode)
                    }
                }
            }

            val isGrid = settingsManager.isGridView.first()
            binding.switchGridLayout.isChecked = isGrid
            binding.switchGridLayout.setOnCheckedChangeListener { _, isChecked ->
                viewLifecycleOwner.lifecycleScope.launch {
                    settingsManager.setGridView(isChecked)
                }
            }
        }
    }

    private fun setupDataManagement() {
        binding.btnExportData.setOnClickListener {
            exportLauncher.launch("knotes_backup_${System.currentTimeMillis()}.json")
        }

        binding.btnImportData.setOnClickListener {
            importLauncher.launch(arrayOf("application/json"))
        }

        binding.btnClearTrash.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear Trash?")
                .setMessage("All notes in trash will be permanently deleted.")
                .setPositiveButton("Clear") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        syncRepository.clearTrash()
                        Toast.makeText(requireContext(), "Trash cleared", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnClearAllData.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete All Local Data?")
                .setMessage("This will permanently delete all notes, tasks, and folders from this device. This cannot be undone.")
                .setPositiveButton("Delete Everything") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        clearAllDataUseCase().onSuccess {
                            Toast.makeText(requireContext(), "All local data deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnDeleteAccount.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Cloud Account?")
                .setMessage("This will permanently delete your account and all synced data from the cloud. Local data will remain unless manually cleared.")
                .setPositiveButton("Delete Account") { _, _ ->
                    auth?.currentUser?.delete()?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
                            updateAuthVisibility()
                        } else {
                            Toast.makeText(requireContext(), "Failed to delete account: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun updateAuthVisibility() {
        val user = auth?.currentUser
        val isSignedIn = user != null
        binding.layoutSignedIn.isVisible = isSignedIn
        binding.layoutSignedOut.isVisible = !isSignedIn

        if (isSignedIn) {
            binding.tvUserEmail.text = user?.email ?: "Anonymous User"
            viewLifecycleOwner.lifecycleScope.launch {
                settingsManager.lastSynced.first().let { timestamp ->
                    updateSyncTimestampUI(timestamp)
                }
            }
        }
    }

    private fun updateSyncTimestampUI(timestamp: Long) {
        if (timestamp == 0L) {
            binding.tvSyncStatus.text = "Last synced: Never"
        } else {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            binding.tvSyncStatus.text = "Last synced: ${sdf.format(Date(timestamp))}"
        }
    }

    private fun observeSyncState() {
        viewLifecycleOwner.lifecycleScope.launch {
            syncRepository.syncState.collectLatest { state ->
                binding.progressSync.isVisible = state is SyncRepository.SyncState.Syncing
                binding.btnSyncNow.isEnabled = state !is SyncRepository.SyncState.Syncing
                
                when (state) {
                    is SyncRepository.SyncState.Success -> {
                        updateSyncTimestampUI(state.lastSynced)
                    }
                    is SyncRepository.SyncState.Error -> {
                        Toast.makeText(requireContext(), "Sync error: ${state.message}", Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun setupAiSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            val isEnabled = settingsManager.isAiEnabled.first()
            binding.switchAiEnabled.isChecked = isEnabled
            
            binding.switchAiEnabled.setOnCheckedChangeListener { _, isChecked ->
                viewLifecycleOwner.lifecycleScope.launch {
                    settingsManager.setAiEnabled(isChecked)
                    if (isChecked) {
                        // If they enable it, we check if they gave consent before
                        val consent = settingsManager.aiConsentGiven.first()
                        if (!consent) {
                            Toast.makeText(requireContext(), "AI enabled. You will be asked for consent on first use.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            
            binding.btnRevokeConsent.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    settingsManager.setAiConsentGiven(false)
                    settingsManager.setAiEnabled(false)
                    binding.switchAiEnabled.isChecked = false
                    Toast.makeText(requireContext(), "AI settings and consent reset.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeNetworkState() {
        viewLifecycleOwner.lifecycleScope.launch {
            connectivityObserver.observe().collect { status ->
                val isAvailable = status == ConnectivityObserver.Status.Available
                if (!isAvailable) {
                    binding.tvSyncStatus.text = "Sync Unavailable: No Network"
                    binding.btnSyncNow.isEnabled = false
                } else {
                    updateAuthVisibility() // Restore normal state
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
