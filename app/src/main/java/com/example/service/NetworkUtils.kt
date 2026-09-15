package com.example.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

object NetworkUtils {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    /**
     * Checks if the device has an active, working network connection (Wi-Fi or Cellular)
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Performs a real TCP socket connection to verify host reachability and measure true latency.
     * Returns true latency in ms, or 999 if unreachable / offline.
     */
    suspend fun pingHost(host: String, port: Int, timeoutMs: Int = 3000): Int {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            var socket: Socket? = null
            try {
                // First resolve host
                val resolvedAddress = InetAddress.getByName(host)
                socket = Socket()
                socket.connect(InetSocketAddress(resolvedAddress, port), timeoutMs)
                val latency = (System.currentTimeMillis() - startTime).toInt()
                latency.coerceAtLeast(1)
            } catch (e: Exception) {
                // Return 999 indicating timeout or unreachable
                999
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Tests real socket handshake with remote server before establishing tunnel.
     * Returns true with latency, or throws exception with actual failure reason.
     */
    suspend fun testSocketHandshake(host: String, port: Int, timeoutMs: Int = 4000): Result<Int> {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            var socket: Socket? = null
            try {
                val address = InetAddress.getByName(host)
                socket = Socket()
                socket.connect(InetSocketAddress(address, port), timeoutMs)
                val elapsed = (System.currentTimeMillis() - startTime).toInt()
                Result.success(elapsed.coerceAtLeast(1))
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {}
            }
        }
    }

    data class HostCheckResult(
        val host: String,
        val statusCode: Int,
        val statusMessage: String,
        val responseHeaders: Map<String, String>,
        val isWorkingBugHost: Boolean
    )

    /**
     * Checks HTTP/HTTPS response of a bug host
     */
    suspend fun checkBugHost(host: String): HostCheckResult {
        return withContext(Dispatchers.IO) {
            val cleanHost = host.removePrefix("http://").removePrefix("https://").trim()
            val url = "https://$cleanHost/"

            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/109.0 Firefox/118.0")
                    .header("Accept", "*/*")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val headers = mutableMapOf<String, String>()
                    for (i in 0 until response.headers.size) {
                        headers[response.headers.name(i)] = response.headers.value(i)
                    }

                    val code = response.code
                    val isBug = code in 200..399 || code == 101

                    HostCheckResult(
                        host = cleanHost,
                        statusCode = code,
                        statusMessage = response.message.ifEmpty { "HTTP $code" },
                        responseHeaders = headers,
                        isWorkingBugHost = isBug
                    )
                }
            } catch (e: Exception) {
                try {
                    val httpUrl = "http://$cleanHost/"
                    val req = Request.Builder().url(httpUrl).build()
                    httpClient.newCall(req).execute().use { resp ->
                        HostCheckResult(
                            host = cleanHost,
                            statusCode = resp.code,
                            statusMessage = resp.message.ifEmpty { "HTTP ${resp.code}" },
                            responseHeaders = emptyMap(),
                            isWorkingBugHost = resp.code in 200..399
                        )
                    }
                } catch (e2: Exception) {
                    HostCheckResult(
                        host = cleanHost,
                        statusCode = 0,
                        statusMessage = "Offline / Connection Failed",
                        responseHeaders = emptyMap(),
                        isWorkingBugHost = false
                    )
                }
            }
        }
    }

    /**
     * Fetches real current public IP address
     */
    suspend fun fetchPublicIp(): String {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.ipify.org")
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.string()?.trim() ?: "Offline"
                    } else {
                        "Offline"
                    }
                }
            } catch (e: Exception) {
                "Offline"
            }
        }
    }
}
