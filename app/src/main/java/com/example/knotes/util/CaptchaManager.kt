package com.example.knotes.util

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

data class CaptchaChallenge(
    val question: String,
    val answer: String
)

@Singleton
class CaptchaManager @Inject constructor() {

    private var failedAttempts = 0
    private var lastFailureTimestamp = 0L

    companion object {
        private const val MAX_UNVERIFIED_ATTEMPTS = 3
        private const val LOCKOUT_DURATION_MS = 15 * 1000L // 15 seconds cooldown
    }

    fun isCaptchaRequired(): Boolean {
        if (failedAttempts >= MAX_UNVERIFIED_ATTEMPTS) {
            return true
        }
        return false
    }

    fun isRateLimited(): Boolean {
        if (failedAttempts >= 5) {
            val elapsed = System.currentTimeMillis() - lastFailureTimestamp
            if (elapsed < LOCKOUT_DURATION_MS) {
                return true
            }
        }
        return false
    }

    fun getRemainingLockoutSeconds(): Int {
        val elapsed = System.currentTimeMillis() - lastFailureTimestamp
        val remainingMs = LOCKOUT_DURATION_MS - elapsed
        return if (remainingMs > 0) (remainingMs / 1000).toInt() + 1 else 0
    }

    fun recordFailedAttempt() {
        failedAttempts++
        lastFailureTimestamp = System.currentTimeMillis()
    }

    fun recordSuccessfulAttempt() {
        failedAttempts = 0
        lastFailureTimestamp = 0L
    }

    fun generateChallenge(): CaptchaChallenge {
        val num1 = Random.nextInt(1, 15)
        val num2 = Random.nextInt(1, 15)
        val isAddition = Random.nextBoolean()

        return if (isAddition) {
            CaptchaChallenge("$num1 + $num2", (num1 + num2).toString())
        } else {
            val max = maxOf(num1, num2)
            val min = minOf(num1, num2)
            CaptchaChallenge("$max - $min", (max - min).toString())
        }
    }

    fun verifyChallenge(userAnswer: String, challenge: CaptchaChallenge): Boolean {
        return userAnswer.trim() == challenge.answer.trim()
    }
}
