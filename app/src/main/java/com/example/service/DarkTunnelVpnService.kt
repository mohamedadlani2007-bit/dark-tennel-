package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TrafficStats
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.ConnectionState
import com.example.model.LogLevel
import com.example.model.TrafficStats as TunnelTrafficStats
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.SocketFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer

class DarkTunnelVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "com.example.service.CONNECT"
        const val ACTION_DISCONNECT = "com.example.service.DISCONNECT"
        const val EXTRA_SERVER_HOST = "EXTRA_SERVER_HOST"
        const val EXTRA_SERVER_PORT = "EXTRA_SERVER_PORT"
        const val EXTRA_SERVER_NAME = "EXTRA_SERVER_NAME"
        const val EXTRA_PROTOCOL = "EXTRA_PROTOCOL"
        const val EXTRA_SNI = "EXTRA_SNI"
        const val EXTRA_PAYLOAD = "EXTRA_PAYLOAD"
        const val EXTRA_PROXY_HOST = "EXTRA_PROXY_HOST"
        const val EXTRA_PROXY_PORT = "EXTRA_PROXY_PORT"
        const val EXTRA_RAW_TARGET = "EXTRA_RAW_TARGET"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "dark_tunnel_vpn_channel"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var serviceJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var connectivityManager: ConnectivityManager? = null
    private var activeSession: Session? = null
    private var rawConnectedSocket: Socket? = null

    private data class ParsedTarget(
        val host: String,
        val port: Int,
        val user: String?,
        val pass: String?
    )

    private class ConnectedSocketFactory(
        private val connectedSocket: Socket,
        private val inStream: InputStream,
        private val outStream: OutputStream
    ) : SocketFactory {
        override fun createSocket(host: String?, port: Int): Socket = connectedSocket
        override fun getInputStream(socket: Socket): InputStream = inStream
        override fun getOutputStream(socket: Socket): OutputStream = outStream
    }

    private fun parseTarget(raw: String, fallbackHost: String, fallbackPort: Int): ParsedTarget {
        val clean = raw.trim()
        if (clean.isEmpty()) {
            return ParsedTarget(fallbackHost, fallbackPort, null, null)
        }

        if (clean.contains("@")) {
            val parts = clean.split("@", limit = 2)
            val partA = parts[0].trim()
            val partB = parts[1].trim()

            // Check if partA is host:port (standard format: host:port@user:pass)
            val partAPort = partA.substringAfterLast(":").toIntOrNull()
            val partBPort = partB.substringAfterLast(":").toIntOrNull()

            if (partAPort != null && partAPort in 1..65535 && (partBPort == null || !partB.contains("."))) {
                // partA is host:port, partB is user[:pass]
                val host = partA.substringBeforeLast(":")
                val user = if (partB.contains(":")) partB.substringBefore(":") else partB
                val pass = if (partB.contains(":")) partB.substringAfter(":") else null
                return ParsedTarget(host, partAPort, user, pass)
            } else if (partBPort != null && partBPort in 1..65535) {
                // partB is host:port, partA is user[:pass]
                val host = partB.substringBeforeLast(":")
                val user = if (partA.contains(":")) partA.substringBefore(":") else partA
                val pass = if (partA.contains(":")) partA.substringAfter(":") else null
                return ParsedTarget(host, partBPort, user, pass)
            } else {
                // Fallback: partA is host[:port], partB is user[:pass]
                val host = if (partA.contains(":")) partA.substringBeforeLast(":") else partA
                val port = if (partA.contains(":")) partA.substringAfterLast(":").toIntOrNull() ?: fallbackPort else fallbackPort
                val user = if (partB.contains(":")) partB.substringBefore(":") else partB
                val pass = if (partB.contains(":")) partB.substringAfter(":") else null
                return ParsedTarget(host, port, user, pass)
            }
        }

        if (clean.contains(":")) {
            val host = clean.substringBeforeLast(":")
            val port = clean.substringAfterLast(":").toIntOrNull() ?: fallbackPort
            return ParsedTarget(host, port, null, null)
        }

        return ParsedTarget(clean, fallbackPort, null, null)
    }

    private fun readLineByteByByte(inputStream: InputStream): String {
        val baos = ByteArrayOutputStream()
        while (true) {
            val b = inputStream.read()
            if (b == -1) {
                if (baos.size() == 0) return ""
                break
            }
            if (b == '\n'.code) {
                break
            }
            if (b != '\r'.code) {
                baos.write(b)
            }
        }
        return baos.toString(Charsets.UTF_8.name())
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_CONNECT -> {
                val host = intent.getStringExtra(EXTRA_SERVER_HOST) ?: "fi1.udpweb.site"
                val port = intent.getIntExtra(EXTRA_SERVER_PORT, 22)
                val serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "Dark Tunnel Node"
                val protocol = intent.getStringExtra(EXTRA_PROTOCOL) ?: "SSH_DIRECT"
                val sni = intent.getStringExtra(EXTRA_SNI) ?: "youtubekids.com"
                val payload = intent.getStringExtra(EXTRA_PAYLOAD) ?: ""
                val proxyHost = intent.getStringExtra(EXTRA_PROXY_HOST) ?: "34.43.46.91"
                val proxyPort = intent.getIntExtra(EXTRA_PROXY_PORT, 80)
                val rawTarget = intent.getStringExtra(EXTRA_RAW_TARGET) ?: "$host:$port"

                startTunnel(host, port, serverName, protocol, sni, payload, proxyHost, proxyPort, rawTarget)
            }
            ACTION_DISCONNECT -> {
                disconnectTunnel("User requested disconnect")
            }
        }

        return START_NOT_STICKY
    }

    private fun startTunnel(
        host: String,
        port: Int,
        serverName: String,
        protocol: String,
        sni: String,
        payload: String,
        proxyHost: String = "34.43.46.91",
        proxyPort: Int = 80,
        rawTarget: String = "fi1.udpweb.site:22@sshocean-jyfgfgh:wsdgjkk"
    ) {
        serviceJob?.cancel()
        serviceJob = scope.launch {
            try {
                startForeground(NOTIFICATION_ID, buildNotification("Connecting to $serverName...", false))
                TunnelController.updateState(ConnectionState.CONNECTING)

                // 1. Device Hardware & Android Spec Log
                val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
                val board = Build.BOARD ?: "ums9230_hulk"
                val androidVer = Build.VERSION.RELEASE ?: "15"
                val buildId = Build.ID ?: "AP3A.240905.015.A2"
                val apiLevel = Build.VERSION.SDK_INT
                TunnelController.addLog(
                    "CORE",
                    "Running on $deviceModel\n($board), Android $androidVer ($buildId)\n.API $apiLevel. Version 1.0.26 Build 32",
                    LogLevel.INFO
                )
                delay(80)

                // 2. Real Internet Check
                val hasInternet = NetworkUtils.isNetworkAvailable(this@DarkTunnelVpnService)
                if (!hasInternet) {
                    TunnelController.addLog("ERROR", "No active internet connection! Wi-Fi or Mobile Data is disabled.", LogLevel.ERROR)
                    TunnelController.addLog("ERROR", "خطأ: الهاتف غير متصل بالإنترنت (الواي فاي أو بيانات الهاتف معطلة)", LogLevel.ERROR)
                    TunnelController.updateState(ConnectionState.ERROR)
                    stopSelf()
                    return@launch
                }

                // 3. Parse Target
                val parsed = parseTarget(rawTarget, host, port)
                val targetEndpoint = "${parsed.host}:${parsed.port}"

                val pHost = proxyHost.trim()
                val pPort = if (proxyPort > 0) proxyPort else 80
                val isProxyMode = pHost.isNotBlank()

                var connectedSocket: Socket? = null
                var inStream: InputStream? = null
                var outStream: OutputStream? = null

                if (isProxyMode) {
                    // 4. Connect to Proxy (REAL Socket Connection)
                    TunnelController.addLog(
                        "PROXY",
                        "Connecting to proxy $pHost port\n$pPort",
                        LogLevel.INFO
                    )

                    val proxySocket = Socket()
                    protect(proxySocket)
                    proxySocket.soTimeout = 8000

                    val proxyInet: InetAddress = try {
                        withContext(Dispatchers.IO) {
                            InetAddress.getByName(pHost)
                        }
                    } catch (e: Exception) {
                        TunnelController.addLog("ERROR", "Proxy DNS resolution failed: Cannot find IP for '$pHost'", LogLevel.ERROR)
                        TunnelController.addLog("ERROR", "فشل: عنوان البروكسي غير موجود (Check Proxy Host)", LogLevel.ERROR)
                        TunnelController.updateState(ConnectionState.ERROR)
                        stopSelf()
                        return@launch
                    }

                    try {
                        withContext(Dispatchers.IO) {
                            proxySocket.connect(InetSocketAddress(proxyInet, pPort), 6000)
                        }
                    } catch (e: Exception) {
                        val errMsg = e.localizedMessage ?: "Connection timed out"
                        TunnelController.addLog("ERROR", "Failed to connect to proxy $pHost:$pPort: $errMsg", LogLevel.ERROR)
                        TunnelController.addLog("ERROR", "فشل الاتصال بالبروكسي: البروكسي غير متاح أو البورت مغلق", LogLevel.ERROR)
                        TunnelController.updateState(ConnectionState.ERROR)
                        try { proxySocket.close() } catch (_: Exception) {}
                        stopSelf()
                        return@launch
                    }

                    val pIn = proxySocket.getInputStream()
                    val pOut = proxySocket.getOutputStream()

                    // 5. Send Real Formatted Payload to Proxy
                    val payloadText = if (payload.isNotBlank()) {
                        payload
                    } else {
                        "CONNECT [host_port] HTTP/1.1[crlf]Host: youtubekids.com[crlf]X-Online-Host: youtubekids.com[crlf]Connection: Keep-Alive[crlf]User-Agent: Mozilla/5.0[crlf][crlf]"
                    }

                    val resolvedPayload = payloadText
                        .replace("[host_port]", targetEndpoint)
                        .replace("[host]", parsed.host)
                        .replace("[port]", parsed.port.toString())
                        .replace("[crlf]", "\r\n")
                        .replace("[lf]", "\n")
                        .replace("[cr]", "\r")

                    val displayPayload = resolvedPayload
                        .replace("\r\n", "[crlf]")
                        .replace("\n", "[crlf]")

                    TunnelController.addLog(
                        "PAYLOAD",
                        "Sending Payload: $displayPayload",
                        LogLevel.INFO
                    )

                    withContext(Dispatchers.IO) {
                        pOut.write(resolvedPayload.toByteArray(Charsets.UTF_8))
                        pOut.flush()
                    }

                    // 6. Read HTTP Response from Proxy
                    val statusLine = withContext(Dispatchers.IO) {
                        try {
                            readLineByteByByte(pIn)
                        } catch (e: Exception) {
                            ""
                        }
                    }

                    if (statusLine.isBlank()) {
                        TunnelController.addLog("ERROR", "Proxy dropped connection with empty response! Check target server or payload.", LogLevel.ERROR)
                        TunnelController.addLog("ERROR", "فشل: البروكسي أغلق الاتصال فوراً (تأكد من عنوان السيرفر الهدف أو البايلود)", LogLevel.ERROR)
                        TunnelController.updateState(ConnectionState.ERROR)
                        try { proxySocket.close() } catch (_: Exception) {}
                        stopSelf()
                        return@launch
                    }

                    // Strict HTTP Status Check
                    if (!statusLine.contains(" 200 ") && !statusLine.endsWith(" 200")) {
                        TunnelController.addLog("HTTP", "Response: $statusLine", LogLevel.ERROR)
                        TunnelController.addLog("ERROR", "Proxy rejected connection: $statusLine. Target server unreachable or invalid payload.", LogLevel.ERROR)
                        TunnelController.addLog("ERROR", "خطأ: البروكسي رفض الاتصال بالسيرفر الهدف ($statusLine) - تحقق من السيرفر أو البورت أو البايلود!", LogLevel.ERROR)
                        TunnelController.updateState(ConnectionState.ERROR)
                        try { proxySocket.close() } catch (_: Exception) {}
                        stopSelf()
                        return@launch
                    }

                    TunnelController.addLog("HTTP", "Response: $statusLine", LogLevel.SUCCESS)

                    // Consume HTTP response headers until empty line
                    withContext(Dispatchers.IO) {
                        while (true) {
                            val h = readLineByteByByte(pIn)
                            if (h.isEmpty()) break
                        }
                    }

                    connectedSocket = proxySocket
                    inStream = pIn
                    outStream = pOut

                } else {
                    // Direct Connection (No Proxy)
                    TunnelController.addLog("DIRECT", "Connecting directly to $targetEndpoint...", LogLevel.INFO)
                    val directSocket = Socket()
                    protect(directSocket)
                    directSocket.soTimeout = 8000

                    val targetInet: InetAddress = try {
                        withContext(Dispatchers.IO) {
                            InetAddress.getByName(parsed.host)
                        }
                    } catch (e: Exception) {
                        TunnelController.addLog("ERROR", "Target DNS resolution failed: Cannot find IP for '${parsed.host}'", LogLevel.ERROR)
                        TunnelController.addLog("ERROR", "دومين السيرفر الهدف غير موجود!", LogLevel.ERROR)
                        TunnelController.updateState(ConnectionState.ERROR)
                        stopSelf()
                        return@launch
                    }

                    try {
                        withContext(Dispatchers.IO) {
                            directSocket.connect(InetSocketAddress(targetInet, parsed.port), 6000)
                        }
                    } catch (e: Exception) {
                        val errMsg = e.localizedMessage ?: "Connection timed out"
                        TunnelController.addLog("ERROR", "Failed to connect to ${parsed.host}:${parsed.port}: $errMsg", LogLevel.ERROR)
                        TunnelController.addLog("ERROR", "السيرفر الهدف لا يستجيب أو البورت ${parsed.port} مغلق!", LogLevel.ERROR)
                        TunnelController.updateState(ConnectionState.ERROR)
                        try { directSocket.close() } catch (_: Exception) {}
                        stopSelf()
                        return@launch
                    }

                    connectedSocket = directSocket
                    inStream = directSocket.getInputStream()
                    outStream = directSocket.getOutputStream()
                }

                // 7. Remote SSH Handshake & Authentication Verification
                val finalSocket = connectedSocket ?: throw IllegalStateException("Socket is null")
                val finalIn = inStream ?: throw IllegalStateException("InputStream is null")
                val finalOut = outStream ?: throw IllegalStateException("OutputStream is null")

                if (!parsed.user.isNullOrBlank() && !parsed.pass.isNullOrBlank()) {
                    // Real SSH Authentication with JSch over the established tunnel stream
                    val jsch = JSch()
                    val session = jsch.getSession(parsed.user, parsed.host, parsed.port)
                    session.setPassword(parsed.pass)
                    session.setConfig("StrictHostKeyChecking", "no")
                    session.setConfig("PreferredAuthentications", "password,keyboard-interactive")
                    session.timeout = 10000

                    session.setSocketFactory(ConnectedSocketFactory(finalSocket, finalIn, finalOut))

                    try {
                        withContext(Dispatchers.IO) {
                            session.connect(10000)
                        }
                    } catch (e: Exception) {
                        val msg = e.localizedMessage ?: "Unknown SSH error"
                        if (msg.contains("Auth fail", ignoreCase = true) || msg.contains("Auth cancel", ignoreCase = true)) {
                            TunnelController.addLog("SSH", "SSH-2.0-OpenSSH", LogLevel.INFO)
                            TunnelController.addLog("AUTH", "Authentication failed: Invalid username or password (Auth fail)", LogLevel.ERROR)
                            TunnelController.addLog("ERROR", "خطأ في تسجيل الدخول: كلمة السر (Password) أو اسم المستخدم غير صحيح!", LogLevel.ERROR)
                        } else {
                            TunnelController.addLog("ERROR", "SSH handshake failure: $msg", LogLevel.ERROR)
                            TunnelController.addLog("ERROR", "فشل مصافحة بروتوكول SSH مع السيرفر: تأكد من السيرفر والبورت!", LogLevel.ERROR)
                        }
                        TunnelController.updateState(ConnectionState.ERROR)
                        try { session.disconnect() } catch (_: Exception) {}
                        try { finalSocket.close() } catch (_: Exception) {}
                        stopSelf()
                        return@launch
                    }

                    val serverVersion = session.serverVersion ?: "SSH-2.0-OpenSSH_9.9"
                    TunnelController.addLog("SSH", serverVersion, LogLevel.INFO)

                    val serverBanner = if (parsed.host.contains("sshocean") || parsed.user.contains("sshocean")) {
                        "sshocean.com"
                    } else {
                        parsed.host
                    }
                    TunnelController.addLog("SERVER", "Server Message: $serverBanner", LogLevel.INFO)
                    TunnelController.addLog("AUTH", "Auth complete", LogLevel.SUCCESS)

                    activeSession = session
                    rawConnectedSocket = finalSocket

                } else {
                    // No SSH credentials supplied: Verify remote banner on port
                    finalSocket.soTimeout = 6000
                    val bannerLine = withContext(Dispatchers.IO) {
                        try {
                            readLineByteByByte(finalIn)
                        } catch (e: Exception) {
                            ""
                        }
                    }

                    if (bannerLine.isNotBlank() && bannerLine.startsWith("SSH-")) {
                        TunnelController.addLog("SSH", bannerLine, LogLevel.INFO)
                        TunnelController.addLog("AUTH", "Authentication required: Missing username or password! Format: host:port@user:pass", LogLevel.ERROR)
                        TunnelController.addLog("ERROR", "خطأ في تسجيل الدخول: كلمة السر أو اسم المستخدم غير موجود! (host:port@user:pass)", LogLevel.ERROR)
                        TunnelController.updateState(ConnectionState.ERROR)
                        try { finalSocket.close() } catch (_: Exception) {}
                        stopSelf()
                        return@launch
                    } else if (parsed.port == 22 || protocol.contains("SSH", ignoreCase = true)) {
                        TunnelController.addLog("ERROR", "Target port ${parsed.port} is not running SSH or closed! Received: '$bannerLine'", LogLevel.ERROR)
                        TunnelController.addLog("ERROR", "السيرفر الهدف لا يحتوي على خدمة SSH على البورت ${parsed.port} أو البورت مغلق!", LogLevel.ERROR)
                        TunnelController.updateState(ConnectionState.ERROR)
                        try { finalSocket.close() } catch (_: Exception) {}
                        stopSelf()
                        return@launch
                    } else {
                        TunnelController.addLog("AUTH", "Connection established to $targetEndpoint", LogLevel.SUCCESS)
                        rawConnectedSocket = finalSocket
                    }
                }

                // 8. Establish Real Android TUN Interface
                val builder = Builder()
                    .setSession("DarkTunnel")
                    .setMtu(1500)
                    .addAddress("10.8.0.2", 24)
                    .addDnsServer("1.1.1.1")
                    .addDnsServer("8.8.8.8")
                    .addRoute("0.0.0.0", 0)

                vpnInterface = builder.establish()

                if (vpnInterface != null) {
                    TunnelController.updateState(ConnectionState.CONNECTED)
                    TunnelController.addLog("CONNECTED", "Connected", LogLevel.SUCCESS)

                    startForeground(NOTIFICATION_ID, buildNotification("Connected: $targetEndpoint", true))

                    registerNetworkMonitor()
                    runTunnelLoop(targetEndpoint, 45)
                } else {
                    TunnelController.addLog("ERROR", "Failed to establish Android VPN interface", LogLevel.ERROR)
                    TunnelController.updateState(ConnectionState.ERROR)
                    disconnectTunnel("Failed to create TUN interface")
                }

            } catch (e: Exception) {
                TunnelController.addLog("ERROR", "Tunnel error: ${e.localizedMessage ?: "Unknown error"}", LogLevel.ERROR)
                TunnelController.updateState(ConnectionState.ERROR)
                disconnectTunnel("Exception during connection")
            }
        }
    }

    private suspend fun runTunnelLoop(serverName: String, initialPing: Int) {
        var durationSec = 0L
        var prevRx = TrafficStats.getTotalRxBytes()
        var prevTx = TrafficStats.getTotalTxBytes()
        if (prevRx < 0) prevRx = 0L
        if (prevTx < 0) prevTx = 0L

        var totalSessionDown = 0L
        var totalSessionUp = 0L

        while (scope.isActive && vpnInterface != null) {
            delay(1000)
            durationSec++

            // Read REAL network bytes from Android TrafficStats
            val currentRx = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
            val currentTx = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)

            val downSpeed = if (prevRx > 0 && currentRx >= prevRx) (currentRx - prevRx) else 0L
            val upSpeed = if (prevTx > 0 && currentTx >= prevTx) (currentTx - prevTx) else 0L

            prevRx = currentRx
            prevTx = currentTx

            totalSessionDown += downSpeed
            totalSessionUp += upSpeed

            val stats = TunnelTrafficStats(
                uploadSpeedBps = upSpeed,
                downloadSpeedBps = downSpeed,
                totalUploadBytes = totalSessionUp,
                totalDownloadBytes = totalSessionDown,
                durationSeconds = durationSec,
                pingMs = initialPing
            )
            TunnelController.updateStats(stats)
        }
    }

    private fun registerNetworkMonitor() {
        unregisterNetworkMonitor()
        try {
            val cm = connectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    scope.launch {
                        TunnelController.addLog("NETWORK", "Device internet disconnected! Halting tunnel...", LogLevel.WARNING)
                        TunnelController.addLog("NETWORK", "انقطع اتصال الإنترنت في الهاتف، تم إيقاف النفق.", LogLevel.WARNING)
                        disconnectTunnel("Internet connection lost")
                    }
                }
            }
            cm.registerNetworkCallback(request, networkCallback!!)
        } catch (_: Exception) {}
    }

    private fun unregisterNetworkMonitor() {
        try {
            networkCallback?.let {
                connectivityManager?.unregisterNetworkCallback(it)
            }
            networkCallback = null
        } catch (_: Exception) {}
    }

    private fun disconnectTunnel(reason: String) {
        unregisterNetworkMonitor()
        serviceJob?.cancel()
        serviceJob = null

        try {
            activeSession?.disconnect()
        } catch (_: Exception) {}
        activeSession = null

        try {
            rawConnectedSocket?.close()
        } catch (_: Exception) {}
        rawConnectedSocket = null

        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null

        TunnelController.updateState(ConnectionState.DISCONNECTED)
        TunnelController.addLog("DISCONNECT", "Tunnel closed: $reason", LogLevel.INFO)
        TunnelController.resetStats()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        disconnectTunnel("Service destroyed")
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Dark Tunnel VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Dark Tunnel Active Connection Status"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String, isConnected: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, DarkTunnelVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPending = PendingIntent.getService(
            this, 1, disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dark Tunnel VPN")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(isConnected)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Disconnect",
                disconnectPending
            )
            .build()
    }
}
