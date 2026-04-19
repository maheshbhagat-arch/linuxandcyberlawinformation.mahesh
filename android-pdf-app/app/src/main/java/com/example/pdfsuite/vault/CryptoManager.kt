package com.example.pdfsuite.vault

import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun encryptFile(input: File, output: File, pin: String) {
        val key = deriveKey(pin)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))

        val encrypted = cipher.doFinal(input.readBytes())
        output.writeBytes(iv + encrypted)
    }

    fun decryptFile(input: File, output: File, pin: String) {
        val bytes = input.readBytes()
        val iv = bytes.copyOfRange(0, 12)
        val data = bytes.copyOfRange(12, bytes.size)

        val key = deriveKey(pin)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))

        output.writeBytes(cipher.doFinal(data))
    }

    private fun deriveKey(pin: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return SecretKeySpec(digest, "AES")
    }
}
