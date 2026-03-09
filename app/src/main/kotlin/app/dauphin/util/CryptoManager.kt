package app.dauphin.util

import android.util.Base64
import android.util.Log
import app.dauphin.BuildConfig
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private const val TAG = "CryptoManager"
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"

    // Replicate Python's fixed-size bytearray logic
    private fun prepareBuffer(input: String, size: Int): ByteArray {
        val buffer = ByteArray(size)
        val inputBytes = input.toByteArray(Charsets.UTF_8)
        val length = minOf(inputBytes.size, size)
        System.arraycopy(inputBytes, 0, buffer, 0, length)
        return buffer
    }

    private val key: ByteArray
        get() = prepareBuffer(BuildConfig.AES_KEY, 32)

    private val iv: ByteArray
        get() = prepareBuffer(BuildConfig.AES_IV, 16)

    fun encrypt(data: String): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val keySpec = SecretKeySpec(key, "AES")
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            // Match Python: Return only the Base64 of the ciphertext (no IV prepended)
            val result = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            Log.d(TAG, "Encrypting: $data -> $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            ""
        }
    }

    fun decrypt(encryptedData: String): String {
        return try {
            val decoded = Base64.decode(encryptedData, Base64.NO_WRAP)
            
            val cipher = Cipher.getInstance(ALGORITHM)
            val keySpec = SecretKeySpec(key, "AES")
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            
            val decrypted = cipher.doFinal(decoded)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            ""
        }
    }
}
