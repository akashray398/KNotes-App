package com.example.knotes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.color.DynamicColors
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.knotes.databinding.ActivityMainBinding
import com.example.knotes.util.SettingsManager
import com.example.knotes.data.repository.SyncRepository
import com.example.knotes.ui.components.OnboardingScreen
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var syncRepository: SyncRepository

    @Inject
    @JvmField
    var auth: FirebaseAuth? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notification permission denied. Reminders won't show.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkNotificationPermission()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        setupOnboarding()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        binding.bottomNavigation.setupWithNavController(navController)
        setupNavHeader()
        setupDrawerNavigation()

        observeAuthState()
        observeThemeMode()
        handleIntent(intent)
    }

    private fun setupDrawerNavigation() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_theme -> {
                    showThemeDialog()
                    binding.drawerLayout.closeDrawers()
                    true
                }
                else -> {
                    navController.navigate(menuItem.itemId)
                    binding.drawerLayout.closeDrawers()
                    true
                }
            }
        }
    }

    private fun setupNavHeader() {
        val headerView = binding.navigationView.getHeaderView(0)
        val composeNavHeader = headerView.findViewById<ComposeView>(R.id.compose_nav_header)
        composeNavHeader?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val user = auth?.currentUser
                val isSignedIn = user != null
                val displayName = user?.displayName ?: if (isSignedIn) "KNotes Member" else "KNotes Guest"
                val email = user?.email ?: ""
                val streak by settingsManager.currentStreak.collectAsState(initial = 0)

                com.example.knotes.ui.components.NavHeaderCompose(
                    displayName = displayName,
                    email = email,
                    streak = streak,
                    isSignedIn = isSignedIn,
                    onHeaderClick = {
                        navController.navigate(R.id.settingsFragment)
                        binding.drawerLayout.closeDrawers()
                    }
                )
            }
        }
    }

    private fun observeThemeMode() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsManager.themeMode.collect { mode ->
                    val nightMode = when (mode) {
                        1 -> AppCompatDelegate.MODE_NIGHT_NO
                        2 -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                    if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
                        AppCompatDelegate.setDefaultNightMode(nightMode)
                    }
                }
            }
        }
    }

    private fun observeAuthState() {
        auth?.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                syncRepository.startSync()
                Toast.makeText(this, "Cloud Sync Active", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "ACTION_OPEN_NOTE") {
            val noteId = intent.getIntExtra("noteId", -1)
            val bundle = Bundle().apply { putInt("noteId", noteId) }
            navController.navigate(R.id.editNoteFragment, bundle)
            return
        }

        intent?.getStringExtra("navigate_to")?.let { target ->
            when (target) {
                "tasks" -> navController.navigate(R.id.tasksFragment)
                "trash" -> navController.navigate(R.id.trashFragment)
                "new_note" -> {
                    val bundle = Bundle().apply { putInt("noteId", -1) }
                    navController.navigate(R.id.editNoteFragment, bundle)
                }
                "new_task" -> {
                    val bundle = Bundle().apply { putInt("taskId", -1) }
                    navController.navigate(R.id.editTaskFragment, bundle)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun setupOnboarding() {
        binding.composeOnboarding.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val onboardingCompleted by settingsManager.onboardingCompleted.collectAsState(initial = null)
                if (onboardingCompleted == false) {
                    OnboardingScreen(
                        onFinished = {
                            lifecycleScope.launch {
                                settingsManager.setOnboardingCompleted(true)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun showThemeDialog() {
        val themes = arrayOf("System Default", "Light", "Dark")
        lifecycleScope.launch {
            val currentTheme = settingsManager.themeMode.first()
            
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle("Choose Theme")
                .setSingleChoiceItems(themes, currentTheme) { dialog, which ->
                    lifecycleScope.launch {
                        settingsManager.setThemeMode(which)
                    }
                    dialog.dismiss()
                }
                .show()
        }
    }
}
