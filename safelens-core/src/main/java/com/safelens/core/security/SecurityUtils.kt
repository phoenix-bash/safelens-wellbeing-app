package com.safelens.core.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {

    /**
     * Verifies that the caller's Linux UID shares the exact same developer signature as this app.
     * Prevents any unauthorized local application from spoofing IPC messages.
     */
    fun isCallerSignatureMatching(context: Context): Boolean {
        val callingUid = Binder.getCallingUid()
        val currentUid = android.os.Process.myUid()

        if (callingUid == currentUid) return true

        val pm = context.packageManager
        return pm.checkSignatures(callingUid, currentUid) == PackageManager.SIGNATURE_MATCH
    }

    /**
     * Compute SHA-256 hash of a string (useful for hashing media paths or tokens).
     */
    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generates an HMAC-SHA256 signature for payload verification.
     */
    fun signPayload(payload: String, secretKey: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(keySpec)
        val rawHmac = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return rawHmac.joinToString("") { "%02x".format(it) }
    }
}
