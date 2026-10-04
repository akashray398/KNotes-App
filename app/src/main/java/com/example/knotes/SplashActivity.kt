package com.example.knotes

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import com.example.knotes.ui.splash.SplashScreenCompose
import com.example.knotes.util.PreferenceManager
import com.example.knotes.util.SecurityManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var securityManager: SecurityManager

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SplashScreenCompose()
            }
        }
        setContentView(composeView)

        // Delayed transition (2 seconds)
        lifecycleScope.launch {
            delay(2000)
            checkSecurityAndProceed()
        }
    }

    private fun checkSecurityAndProceed() {
        if (preferenceManager.isAppLocked() && securityManager.isBiometricAvailable()) {
            securityManager.showBiometricPrompt(
                activity = this,
                onSuccess = {
                    proceedToMain()
                },
                onError = { error ->
                    Toast.makeText(this, "Authentication failed: $error", Toast.LENGTH_SHORT).show()
                    finish()
                }
            )
        } else {
            proceedToMain()
        }
    }

    private fun proceedToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
