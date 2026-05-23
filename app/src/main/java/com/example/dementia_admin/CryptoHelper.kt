package com.example.dementia_admin

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {
    private const val SECRET_KEY = "MeineGeheimeDemenzAppSchluessel!"
    private const val INIT_VECTOR = "RandomInitVector" // 16 Zeichen

    fun encrypt(value: String): String {
        return try {
            val iv = IvParameterSpec(INIT_VECTOR.toByteArray(charset("UTF-8")))
            val skeySpec = SecretKeySpec(SECRET_KEY.toByteArray(charset("UTF-8")), "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv)

            val encrypted = cipher.doFinal(value.toByteArray())
            Base64.encodeToString(encrypted, Base64.DEFAULT)
        } catch (ex: Exception) {
            value
        }
    }

    fun decrypt(encrypted: String): String {
        return try {
            val iv = IvParameterSpec(INIT_VECTOR.toByteArray(charset("UTF-8")))
            val skeySpec = SecretKeySpec(SECRET_KEY.toByteArray(charset("UTF-8")), "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv)

            val original = cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT))
            String(original)
        } catch (ex: Exception) {
            encrypted
        }
    }
}