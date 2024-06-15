package org.sixtysix.security

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Util {
    private val encoder = Base64.getEncoder()
    private val keyGenerator = KeyGenerator.getInstance("AES").also { it.init(128) }
    private val keyGenMutex = Mutex()

    private val messageDigest = MessageDigest.getInstance("MD5")
    private val digestMutex = Mutex()

    private val ivParameterSpec = IvParameterSpec(ByteArray(16))

    suspend fun newSecret(): String = keyGenMutex.withLock {
        keyGenerator.generateKey().let { encoder.encodeToString(it.encoded) }
    }

    fun String.toKey() = SecretKeySpec(Base64.getDecoder().decode(this), "AES")

    fun encrypt(data: ByteArray, keys: List<SecretKey>) = applyCipher(data, keys, Cipher.ENCRYPT_MODE)

    fun decrypt(encryptedData: ByteArray, keys: List<SecretKey>) = applyCipher(encryptedData, keys.reversed(), Cipher.DECRYPT_MODE)

    // TODO: error handling
    private fun applyCipher(data: ByteArray, keys: List<SecretKey>, mode: Int): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        return keys.fold(data) { acc, key ->
            cipher.init(mode, key, ivParameterSpec)
            cipher.doFinal(acc)
        }
    }

    suspend fun String.hash() = digestMutex.withLock {
        messageDigest.digest(toByteArray())
            .joinToString(separator = "", transform = "%02x"::format)
    }
}