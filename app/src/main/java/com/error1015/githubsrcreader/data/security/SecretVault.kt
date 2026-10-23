package com.error1015.githubsrcreader.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 用 Android Keystore 里的 AES-256-GCM 密钥加解密敏感字符串（GitHub Token 等）。
 *
 * - 密钥由系统生成并保存在 Keystore（有 TEE / StrongBox 时不出安全硬件），应用只能“使用”不能“读出”；
 * - 落盘的只有 `v1:iv密文:数据密文`，即使 SharedPreferences 被拷走也解不开；
 * - 密文带 GCM 认证标签，被篡改会解密失败而不是返回错误内容。
 */
object SecretVault {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "ghsr_secret_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_BITS = 128
    private const val PREFIX = "v1"

    /** 加密失败返回 null（调用方不要退化成明文存储）。 */
    fun encrypt(plain: String): String? = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val payload = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val encoder = Base64.getEncoder()
        "$PREFIX:${encoder.encodeToString(cipher.iv)}:${encoder.encodeToString(payload)}"
    }.getOrNull()

    /** 解密失败（密钥失效、密文损坏）返回 null。 */
    fun decrypt(stored: String): String? = runCatching {
        val parts = stored.split(':', limit = 3)
        if (parts.size != 3 || parts[0] != PREFIX) {
            null
        } else {
            val decoder = Base64.getDecoder()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(TAG_BITS, decoder.decode(parts[1])),
            )
            String(cipher.doFinal(decoder.decode(parts[2])), Charsets.UTF_8)
        }
    }.getOrNull()

    /** 丢弃密钥与所有历史密文（当前未使用，保留给「彻底清除敏感数据」场景）。 */
    fun deleteKey() {
        runCatching {
            KeyStore.getInstance(KEYSTORE).apply { load(null) }.deleteEntry(KEY_ALIAS)
        }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }
}
