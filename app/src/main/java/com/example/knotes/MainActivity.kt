package com.example.knotes

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.knotes.databinding.ActivityMainBinding
import com.example.knotes.util.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        binding.bottomNavigation.setupWithNavController(navController)
        binding.navigationView.setupWithNavController(navController)

        // Custom handling for theme only
        binding.navigationView.menu.findItem(R.id.action_theme).setOnMenuItemClickListener {
            showThemeDialog()
            binding.drawerLayout.closeDrawers()
            true
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
                        val mode = when (which) {
                            1 -> AppCompatDelegate.MODE_NIGHT_NO
                            2 -> AppCompatDelegate.MODE_NIGHT_YES
                            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        }
                        AppCompatDelegate.setDefaultNightMode(mode)
                    }
                    dialog.dismiss()
                }
                .show()
        }
    }
}
