package com.deepintv.launcher.data

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * The secret behind the pairing QR. The QR carries a one-time [token]; the first visit that
 * presents it (and only the first) is given a session id, tied to that phone's address, which
 * the page then sends with every request. Everything expires after [ttlMs], and too many wrong
 * guesses lock the session for good. Pure: the clock and the random source can be injected.
 */
class PairingSession(
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val random: SecureRandom = SecureRandom(),
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val maxFailures: Int = 5,
) {
    val token: String = randomHex(16)
    private val startedAt = nowMs()
    private var redeemed = false
    private var failures = 0
    private var sessionId: String? = null
    private var sessionIp: String? = null
    private var sessionExpiresAt = 0L

    @Synchronized
    fun isLocked(): Boolean = failures >= maxFailures

    @Synchronized
    fun isExpired(): Boolean {
        // Before anyone scans, the QR lives [ttlMs] from its creation; after, the session does.
        val deadline = if (sessionId == null) startedAt + ttlMs else sessionExpiresAt
        return nowMs() > deadline
    }

    /** Milliseconds until this pairing (or the session it gave out) expires, 0 once it has. */
    @Synchronized
    fun remainingMs(): Long {
        val deadline = if (sessionId == null) startedAt + ttlMs else sessionExpiresAt
        return (deadline - nowMs()).coerceAtLeast(0L)
    }

    /** Exchanges the QR token for a session id, once. Null for anything else. */
    @Synchronized
    fun redeem(candidate: String?, clientIp: String): String? {
        if (isLocked() || isExpired()) return null
        if (redeemed || !sameSecret(candidate, token)) {
            failures++
            return null
        }
        redeemed = true
        sessionId = randomHex(16)
        sessionIp = clientIp
        sessionExpiresAt = nowMs() + ttlMs
        return sessionId
    }

    /** Whether [candidate] is this pairing's session id, presented from the phone that redeemed it. */
    @Synchronized
    fun isAuthorized(candidate: String?, clientIp: String): Boolean {
        if (isLocked() || isExpired()) return false
        val id = sessionId
        val ok = id != null && clientIp == sessionIp && sameSecret(candidate, id)
        if (!ok) failures++
        return ok
    }

    private fun randomHex(bytes: Int): String =
        ByteArray(bytes).also(random::nextBytes).joinToString("") { "%02x".format(it) }

    /** Constant-time comparison, so response timing says nothing about how much matched. */
    private fun sameSecret(candidate: String?, secret: String): Boolean =
        candidate != null && MessageDigest.isEqual(candidate.toByteArray(), secret.toByteArray())

    companion object {
        const val DEFAULT_TTL_MS = 5 * 60_000L
    }
}
