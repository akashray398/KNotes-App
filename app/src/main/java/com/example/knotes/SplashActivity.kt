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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        lifecycleScope.launch {
            delay(2000)
            checkSecurityAndProceed()
        }
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
        // Background Glow pulse
        binding.ivGlow.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .alpha(0.8f)
            .setDuration(2000)
            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
            .start()

        // Logo Card scale and fade in
        binding.cvLogo.animate()
            .alpha(1f)
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(1000)
            .setStartDelay(200)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .withEndAction {
                binding.cvLogo.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .start()
            }
            .start()

        // App Name slide up and fade in
        binding.tvAppName.translationY = 40f
        binding.tvAppName.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setStartDelay(600)
            .start()

        // Tagline fade in
        binding.tvTagline.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(1000)
            .start()

        // Loading Indicator fade in
        binding.loadingIndicator.animate()
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(1400)
            .start()

        // Particle Animations
        animateParticle(binding.particle1, 2000, -100f)
        animateParticle(binding.particle2, 2500, -150f)
        animateParticle(binding.particle3, 3000, -120f)
    }

    private fun animateParticle(view: View, duration: Long, distance: Float) {
        view.alpha = 0f
        view.animate()
            .alpha(0.6f)
            .translationYBy(distance)
            .setDuration(duration)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                view.animate()
                    .alpha(0f)
                    .setDuration(duration / 2)
                    .withEndAction {
                        view.translationY = 0f
                        animateParticle(view, duration, distance)
                    }
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
