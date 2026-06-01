package com.example.knotes

import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.knotes.databinding.ActivitySplashBinding
import com.example.knotes.util.PreferenceManager
import com.example.knotes.util.SecurityManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    @Inject
    lateinit var securityManager: SecurityManager

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        startAnimations()

        // Delayed transition (2 seconds)
        Handler(Looper.getMainLooper()).postDelayed({
            checkSecurityAndProceed()
        }, 2000)
    }

    private fun setupUI() {
        // Apply Purple Gradient to App Name
        binding.tvAppName.post {
            val paint = binding.tvAppName.paint
            val width = paint.measureText(binding.tvAppName.text.toString())
            val textShader = LinearGradient(
                0f, 0f, width, binding.tvAppName.textSize,
                intArrayOf(
                    ContextCompat.getColor(this, R.color.purple_D0BCFF),
                    ContextCompat.getColor(this, R.color.purple_6750A4)
                ),
                null, Shader.TileMode.CLAMP
            )
            binding.tvAppName.paint.shader = textShader
            binding.tvAppName.invalidate()
        }
    }

    private fun startAnimations() {
        // Logo Card scale and fade in
        binding.cvLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(800)
            .setStartDelay(200)
            .start()

        // App Name fade in
        binding.tvAppName.animate()
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(600)
            .start()

        // Tagline fade in
        binding.tvTagline.animate()
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(1000)
            .start()

        // Loading Indicator fade in
        binding.loadingIndicator.animate()
            .alpha(1f)
            .setDuration(400)
            .setStartDelay(1200)
            .start()

        // Particle Animations
        animateParticle(binding.particle1, 2000)
        animateParticle(binding.particle2, 2500)
        animateParticle(binding.particle3, 3000)
    }

    private fun animateParticle(view: View, duration: Long) {
        view.animate()
            .alpha(0.6f)
            .translationYBy(-30f)
            .setDuration(duration)
            .withEndAction {
                view.animate()
                    .alpha(0f)
                    .translationYBy(30f)
                    .setDuration(duration)
                    .withEndAction { animateParticle(view, duration) }
                    .start()
            }
            .start()
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
                    // In a real app, you might want to show a retry button or secondary PIN
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
