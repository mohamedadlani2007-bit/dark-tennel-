package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class DarkConfigFile(
    val name: String,
    val description: String,
    val isLocked: Boolean,
    val target: String,
    val proxy: String,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis()
)

object DarkConfigManager {
    private const val HEADER = "DARK_TUNNEL_CONFIG_V1::"
    // Security key for encrypting locked configs
    private val AES_KEY = "D4rk_Tunn3l_K3y!9876543210Secur3".toByteArray(StandardCharsets.UTF_8).sliceArray(0 until 16)
    private val AES_IV = "DarkTunnelIV1234".toByteArray(StandardCharsets.UTF_8).sliceArray(0 until 16)

    fun exportToString(
        name: String,
        description: String,
        isLocked: Boolean,
        target: String,
        proxy: String,
        payload: String
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("name", name.ifBlank { "DarkTunnel_Config" })
        root.put("description", description)
        root.put("isLocked", isLocked)
        root.put("createdAt", System.currentTimeMillis())

        if (isLocked) {
            val sensitive = JSONObject().apply {
                put("target", target)
                put("proxy", proxy)
                put("payload", payload)
            }.toString()

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = SecretKeySpec(AES_KEY, "AES")
            val ivSpec = IvParameterSpec(AES_IV)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(sensitive.toByteArray(StandardCharsets.UTF_8))
            val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            root.put("data", encryptedBase64)
        } else {
            root.put("target", target)
            root.put("proxy", proxy)
            root.put("payload", payload)
        }

        val jsonStr = root.toString()
        val encoded = Base64.encodeToString(jsonStr.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        return HEADER + encoded
    }

    fun importFromString(content: String): DarkConfigFile? {
        return try {
            val clean = content.trim()
            val rawJson = if (clean.startsWith(HEADER)) {
                val b64 = clean.removePrefix(HEADER)
                String(Base64.decode(b64, Base64.DEFAULT), StandardCharsets.UTF_8)
            } else if (clean.startsWith("{")) {
                clean
            } else {
                try {
                    String(Base64.decode(clean, Base64.DEFAULT), StandardCharsets.UTF_8)
                } catch (_: Exception) {
                    clean
                }
            }

            val root = JSONObject(rawJson)
            val name = root.optString("name", "DarkTunnel_Config")
            val description = root.optString("description", "")
            val isLocked = root.optBoolean("isLocked", false)

            if (isLocked) {
                val encData = root.getString("data")
                val encBytes = Base64.decode(encData, Base64.DEFAULT)
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                val keySpec = SecretKeySpec(AES_KEY, "AES")
                val ivSpec = IvParameterSpec(AES_IV)
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
                val decrypted = String(cipher.doFinal(encBytes), StandardCharsets.UTF_8)
                val dataObj = JSONObject(decrypted)

                val target = dataObj.optString("target", "")
                val proxy = dataObj.optString("proxy", "")
                val payload = dataObj.optString("payload", "")

                DarkConfigFile(
                    name = name,
                    description = description,
                    isLocked = true,
                    target = target,
                    proxy = proxy,
                    payload = payload,
                    createdAt = root.optLong("createdAt", System.currentTimeMillis())
                )
            } else {
                val target = root.optString("target", "")
                val proxy = root.optString("proxy", "")
                val payload = root.optString("payload", "")

                DarkConfigFile(
                    name = name,
                    description = description,
                    isLocked = false,
                    target = target,
                    proxy = proxy,
                    payload = payload,
                    createdAt = root.optLong("createdAt", System.currentTimeMillis())
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveToShareFile(context: Context, fileName: String, content: String): File? {
        return try {
            val dir = File(context.cacheDir, "configs")
            if (!dir.exists()) dir.mkdirs()
            val cleanName = if (fileName.endsWith(".dark", ignoreCase = true)) fileName else "$fileName.dark"
            val file = File(dir, cleanName)
            FileOutputStream(file).use { it.write(content.toByteArray(StandardCharsets.UTF_8)) }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun readFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(StandardCharsets.UTF_8).readText()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun writeToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray(StandardCharsets.UTF_8))
                stream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
