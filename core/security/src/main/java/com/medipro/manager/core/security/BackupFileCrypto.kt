package com.medipro.manager.core.security

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Password-based AES-GCM encryption for `.medipro` backup files.
 * Format: MAGIC(8) | VERSION(1) | SALT(16) | IV(12) | SHA256(32) | CIPHERTEXT
 */
@Singleton
class BackupFileCrypto @Inject constructor() {

    fun encrypt(plainCompressed: ByteArray, password: CharArray): ByteArray {
        require(password.isNotEmpty()) { "Backup password is required" }
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(GCM_IV_BYTES).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plainCompressed)
        val checksum = sha256(plainCompressed)
        return encodeFile(salt, iv, checksum, ciphertext)
    }

    fun decrypt(fileBytes: ByteArray, password: CharArray): ByteArray {
        require(password.isNotEmpty()) { "Backup password is required" }
        val parsed = parseFile(fileBytes)
        val key = deriveKey(password, parsed.salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, parsed.iv))
        val plain = cipher.doFinal(parsed.ciphertext)
        val actual = sha256(plain)
        require(actual.contentEquals(parsed.contentSha256)) {
            "Backup integrity check failed — wrong password or corrupted file"
        }
        return plain
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    private fun encodeFile(
        salt: ByteArray,
        iv: ByteArray,
        checksum: ByteArray,
        ciphertext: ByteArray,
    ): ByteArray {
        val buffer = ByteBuffer.allocate(
            MAGIC.size + 1 + salt.size + iv.size + checksum.size + ciphertext.size
        )
        buffer.put(MAGIC)
        buffer.put(FILE_VERSION)
        buffer.put(salt)
        buffer.put(iv)
        buffer.put(checksum)
        buffer.put(ciphertext)
        return buffer.array()
    }

    private fun parseFile(bytes: ByteArray): ParsedFile {
        require(bytes.size >= HEADER_SIZE) { "Invalid backup file — too small" }
        val magic = bytes.copyOfRange(0, MAGIC.size)
        require(magic.contentEquals(MAGIC)) { "Invalid backup file — not a MediPro backup" }
        val version = bytes[MAGIC.size]
        require(version == FILE_VERSION) { "Unsupported backup version: $version" }
        var offset = MAGIC.size + 1
        val salt = bytes.copyOfRange(offset, offset + SALT_BYTES).also { offset += SALT_BYTES }
        val iv = bytes.copyOfRange(offset, offset + GCM_IV_BYTES).also { offset += GCM_IV_BYTES }
        val checksum = bytes.copyOfRange(offset, offset + SHA256_BYTES).also { offset += SHA256_BYTES }
        val ciphertext = bytes.copyOfRange(offset, bytes.size)
        return ParsedFile(salt, iv, checksum, ciphertext)
    }

    private data class ParsedFile(
        val salt: ByteArray,
        val iv: ByteArray,
        val contentSha256: ByteArray,
        val ciphertext: ByteArray,
    )

    companion object {
        private val MAGIC = "MEDIPRO1".toByteArray(StandardCharsets.US_ASCII)
        private const val FILE_VERSION: Byte = 1
        private const val SALT_BYTES = 16
        private const val GCM_IV_BYTES = 12
        private const val SHA256_BYTES = 32
        private const val KEY_BITS = 256
        private const val PBKDF2_ITERATIONS = 120_000
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private val HEADER_SIZE = MAGIC.size + 1 + SALT_BYTES + GCM_IV_BYTES + SHA256_BYTES
    }
}
