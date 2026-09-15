package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DarkConfigFile
import com.example.data.DarkConfigManager
import com.example.data.DefaultServers
import com.example.data.ServerExtractor
import com.example.data.TunnelRepository
import com.example.model.ConnectionState
import com.example.model.InjectionMethod
import com.example.model.LogLevel
import com.example.model.PayloadConfig
import com.example.model.TunnelProtocol
import com.example.model.TunnelServer
import com.example.service.NetworkDetector
import com.example.service.NetworkInfo
import com.example.service.NetworkUtils
import com.example.service.TunnelController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TunnelRepository(application)

    val connectionState: StateFlow<ConnectionState> = TunnelController.connectionState
    val trafficStats = TunnelController.trafficStats
    val logs = TunnelController.logs

    val selectedServer = repository.selectedServer
    val allServers = repository.servers
    val payloadConfig = repository.payloadConfig
    val dnsServer = repository.dnsServer
    val udpForwarding = repository.udpForwarding

    // Detected Network & Carrier state
    private val _networkInfo = MutableStateFlow<NetworkInfo?>(null)
    val networkInfo: StateFlow<NetworkInfo?> = _networkInfo.asStateFlow()

    // Filter tab for servers screen: ALL, ONLINE, SSH, V2RAY, SLOWDNS, EXTRACTED
    private val _selectedCategory = MutableStateFlow("ALL")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Filtered servers list
    val filteredServers: StateFlow<List<TunnelServer>> = combine(allServers, _selectedCategory) { list, cat ->
        when (cat) {
            "ONLINE" -> list.filter { it.isOnline }
            "SSH" -> list.filter { it.protocol == TunnelProtocol.SSH_DIRECT || it.protocol == TunnelProtocol.SSH_SSL || it.protocol == TunnelProtocol.SSH_WEBSOCKET }
            "V2RAY" -> list.filter { it.protocol == TunnelProtocol.VMESS || it.protocol == TunnelProtocol.VLESS || it.protocol == TunnelProtocol.TROJAN }
            "SLOWDNS" -> list.filter { it.protocol == TunnelProtocol.SLOWDNS }
            "EXTRACTED" -> list.filter { it.isExtracted || it.isCustom }
            else -> list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Ping testing state
    private val _isTestingPings = MutableStateFlow(false)
    val isTestingPings: StateFlow<Boolean> = _isTestingPings.asStateFlow()

    // Server Extractor Tool State
    private val _extractorInput = MutableStateFlow("")
    val extractorInput: StateFlow<String> = _extractorInput.asStateFlow()

    private val _extractionResult = MutableStateFlow<ServerExtractor.ExtractionResult?>(null)
    val extractionResult: StateFlow<ServerExtractor.ExtractionResult?> = _extractionResult.asStateFlow()

    // Bug Host Checker State
    private val _bugHostInput = MutableStateFlow("m.youtube.com")
    val bugHostInput: StateFlow<String> = _bugHostInput.asStateFlow()

    private val _bugHostChecking = MutableStateFlow(false)
    val bugHostChecking: StateFlow<Boolean> = _bugHostChecking.asStateFlow()

    private val _bugHostResult = MutableStateFlow<NetworkUtils.HostCheckResult?>(null)
    val bugHostResult: StateFlow<NetworkUtils.HostCheckResult?> = _bugHostResult.asStateFlow()

    // Public IP state
    private val _publicIp = MutableStateFlow("Checking...")
    val publicIp: StateFlow<String> = _publicIp.asStateFlow()

    // User feedback notification / snackbar
    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    // DarkTunnel Exact Configuration (Matches User Screenshot)
    private val _targetConfig = MutableStateFlow("fi1.udpweb.site:22@sshocean-jyfgfgh:wsdgjkk")
    val targetConfig: StateFlow<String> = _targetConfig.asStateFlow()

    private val _proxyConfig = MutableStateFlow("34.43.46.91:80")
    val proxyConfig: StateFlow<String> = _proxyConfig.asStateFlow()

    private val _payloadConfigText = MutableStateFlow(
        "CONNECT [host_port] HTTP/1.1[crlf]Host: youtubekids.com[crlf]X-Online-Host: youtubekids.com[crlf]Connection: Keep-Alive[crlf]User-Agent: Mozilla/5.0[crlf][crlf]"
    )
    val payloadConfigText: StateFlow<String> = _payloadConfigText.asStateFlow()

    private val _tunnelModeSubtitle = MutableStateFlow("SSH  •  Performance Mode  •  Proxy -> Target")
    val tunnelModeSubtitle: StateFlow<String> = _tunnelModeSubtitle.asStateFlow()

    // Loaded .dark config file state
    private val _activeConfigFile = MutableStateFlow<DarkConfigFile?>(null)
    val activeConfigFile: StateFlow<DarkConfigFile?> = _activeConfigFile.asStateFlow()

    private var lockedSecretTarget: String? = null
    private var lockedSecretProxy: String? = null
    private var lockedSecretPayload: String? = null

    val isConfigLocked: StateFlow<Boolean> = _activeConfigFile.map { config ->
        config?.isLocked == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun updateTargetConfig(value: String) {
        if (_activeConfigFile.value?.isLocked == true) return
        _targetConfig.value = value
    }

    fun updateProxyConfig(value: String) {
        if (_activeConfigFile.value?.isLocked == true) return
        _proxyConfig.value = value
    }

    fun updatePayloadConfigText(value: String) {
        if (_activeConfigFile.value?.isLocked == true) return
        _payloadConfigText.value = value
    }

    fun updateTunnelModeSubtitle(value: String) {
        _tunnelModeSubtitle.value = value
    }

    fun generateExportContent(name: String, note: String, isLocked: Boolean): String {
        val target = if (_activeConfigFile.value?.isLocked == true) (lockedSecretTarget ?: "") else _targetConfig.value
        val proxy = if (_activeConfigFile.value?.isLocked == true) (lockedSecretProxy ?: "") else _proxyConfig.value
        val payload = if (_activeConfigFile.value?.isLocked == true) (lockedSecretPayload ?: "") else _payloadConfigText.value

        return DarkConfigManager.exportToString(
            name = name,
            description = note,
            isLocked = isLocked,
            target = target,
            proxy = proxy,
            payload = payload
        )
    }

    fun applyImportedConfig(config: DarkConfigFile): Boolean {
        _activeConfigFile.value = config
        if (config.isLocked) {
            lockedSecretTarget = config.target
            lockedSecretProxy = config.proxy
            lockedSecretPayload = config.payload
            _targetConfig.value = "🔒 سيرفر محمي ومغلق بواسطة الصانع"
            _proxyConfig.value = "🔒 بروكسي محمي ومغلق"
            _payloadConfigText.value = "🔒 بايلود محمي ومغلق"
            _tunnelModeSubtitle.value = "ملف كونفيغ: ${config.name} (🔒 محمي)"
            _snackMessage.value = "تم استيراد ملف الكونفيغ المحمي: ${config.name}"
            TunnelController.addLog("CONFIG", "تم استيراد ملف كونفيغ مغلق ومحمي: ${config.name}.dark", LogLevel.SUCCESS)
            if (config.description.isNotBlank()) {
                TunnelController.addLog("NOTE", "رسالة الصانع: ${config.description}", LogLevel.INFO)
            }
        } else {
            lockedSecretTarget = null
            lockedSecretProxy = null
            lockedSecretPayload = null
            _targetConfig.value = config.target
            _proxyConfig.value = config.proxy
            _payloadConfigText.value = config.payload
            _tunnelModeSubtitle.value = "ملف كونفيغ: ${config.name} (مفتوح)"
            _snackMessage.value = "تم استيراد ملف الكونفيغ: ${config.name}"
            TunnelController.addLog("CONFIG", "تم استيراد ملف كونفيغ مفتوح: ${config.name}.dark", LogLevel.SUCCESS)
            if (config.description.isNotBlank()) {
                TunnelController.addLog("NOTE", "رسالة الصانع: ${config.description}", LogLevel.INFO)
            }
        }
        return true
    }

    fun importFromUri(context: Context, uri: Uri): Boolean {
        val content = DarkConfigManager.readFromUri(context, uri) ?: run {
            _snackMessage.value = "تعذر قراءة الملف المحدد من الهاتف!"
            TunnelController.addLog("ERROR", "Failed to read file from storage URI", LogLevel.ERROR)
            return false
        }
        val config = DarkConfigManager.importFromString(content) ?: run {
            _snackMessage.value = "صيغة الملف غير صالحة أو تالفة! تأكد أنه ملف .dark صحيح."
            TunnelController.addLog("ERROR", "Invalid or corrupted .dark file format", LogLevel.ERROR)
            return false
        }
        return applyImportedConfig(config)
    }

    fun importFromClipboard(context: Context): Boolean {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        if (clip.isBlank()) {
            _snackMessage.value = "الحافظة فارغة!"
            return false
        }
        val config = DarkConfigManager.importFromString(clip)
        if (config == null) {
            _snackMessage.value = "النص الموجود في الحافظة ليس كود كونفيغ Dark Tunnel صحيح!"
            return false
        }
        return applyImportedConfig(config)
    }

    fun saveConfigToUri(context: Context, uri: Uri, name: String, note: String, isLocked: Boolean): Boolean {
        val content = generateExportContent(name, note, isLocked)
        val success = DarkConfigManager.writeToUri(context, uri, content)
        if (success) {
            _snackMessage.value = "تم حفظ الملف بنجاح في هاتفك: $name.dark"
            TunnelController.addLog("EXPORT", "Saved config file: $name.dark (Locked: $isLocked)", LogLevel.SUCCESS)
        } else {
            _snackMessage.value = "فشل في حفظ الملف على مسار الهاتف المختار"
            TunnelController.addLog("ERROR", "Failed writing config to URI", LogLevel.ERROR)
        }
        return success
    }

    fun shareConfigFile(context: Context, name: String, note: String, isLocked: Boolean) {
        val content = generateExportContent(name, note, isLocked)
        val file = DarkConfigManager.saveToShareFile(context, name, content)
        if (file != null && file.exists()) {
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "$name.dark - Dark Tunnel Config")
                    val textNote = if (note.isNotBlank()) "\nوصف الكونفيغ: $note" else ""
                    putExtra(Intent.EXTRA_TEXT, "ملف كونفيغ Dark Tunnel: $name.dark$textNote")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "مشاركة ملف الكونفيغ عبر..."))
            } catch (e: Exception) {
                _snackMessage.value = "تعذر فتح نافذة المشاركة: ${e.message}"
            }
        } else {
            _snackMessage.value = "حدث خطأ أثناء إنشاء ملف المشاركة المؤقت"
        }
    }

    fun clearActiveConfig() {
        _activeConfigFile.value = null
        lockedSecretTarget = null
        lockedSecretProxy = null
        lockedSecretPayload = null
        resetToScreenshotDefaults()
        _snackMessage.value = "تمت إزالة الكونفيغ والعودة للوضع اليدوي"
    }

    fun resetToScreenshotDefaults() {
        _activeConfigFile.value = null
        lockedSecretTarget = null
        lockedSecretProxy = null
        lockedSecretPayload = null
        _targetConfig.value = "fi1.udpweb.site:22@sshocean-jyfgfgh:wsdgjkk"
        _proxyConfig.value = "34.43.46.91:80"
        _payloadConfigText.value = "CONNECT [host_port] HTTP/1.1[crlf]Host: youtubekids.com[crlf]X-Online-Host: youtubekids.com[crlf]Connection: Keep-Alive[crlf]User-Agent: Mozilla/5.0[crlf][crlf]"
        _tunnelModeSubtitle.value = "SSH  •  Performance Mode  •  Proxy -> Target"
        _snackMessage.value = "DarkTunnel settings restored to default"
    }

    init {
        loadPublicIp()
        refreshNetworkInfo()
    }

    fun refreshNetworkInfo() {
        _networkInfo.value = NetworkDetector.detect(getApplication())
    }

    fun addCustomServer(
        rawText: String,
        customHost: String = "",
        customPort: Int = 443,
        customUser: String = "",
        customPass: String = "",
        customSni: String = "",
        customProtocol: TunnelProtocol = TunnelProtocol.SSH_SSL
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val serverToAdd: TunnelServer = if (rawText.isNotBlank()) {
                val res = ServerExtractor.extractConfig(rawText.trim())
                if (res.success && res.server != null) {
                    res.server.copy(isCustom = true)
                } else {
                    _snackMessage.value = "تعذر قراءة الصيغة، يرجى التأكد من البيانات."
                    return@launch
                }
            } else {
                val cleanHost = customHost.trim()
                if (cleanHost.isEmpty()) {
                    _snackMessage.value = "يرجى كتابة عنوان السيرفر (IP أو Host Domain)"
                    return@launch
                }
                TunnelServer(
                    id = "custom_${System.currentTimeMillis()}",
                    name = "Custom Node ($cleanHost:$customPort)",
                    country = "Custom Node",
                    countryCode = "XX",
                    flagEmoji = "⚡",
                    host = cleanHost,
                    port = customPort,
                    protocol = customProtocol,
                    username = customUser.trim().ifEmpty { "darktunnel" },
                    password = customPass.trim().ifEmpty { "free" },
                    sni = customSni.trim(),
                    isCustom = true,
                    isExtracted = true,
                    description = "سيرفر مخصص مضاف يدوياً على المنفذ $customPort",
                    rawConfig = if (customUser.isNotEmpty()) "$cleanHost:$customPort@$customUser:$customPass" else "$cleanHost:$customPort"
                )
            }

            // Real socket handshake check to test if server is alive
            TunnelController.addLog("TEST", "Testing connection to ${serverToAdd.host}:${serverToAdd.port}...", LogLevel.INFO)
            val handshake = NetworkUtils.testSocketHandshake(serverToAdd.host, serverToAdd.port, timeoutMs = 3500)
            val ping = if (handshake.isSuccess) handshake.getOrDefault(45) else 999
            val testedServer = serverToAdd.copy(pingMs = ping)

            repository.addServer(testedServer)
            repository.selectServer(testedServer)

            if (handshake.isSuccess) {
                _snackMessage.value = "✓ السيرفر شغال (${ping}ms) وتمت إضافته وتحديده بنجاح!"
                TunnelController.addLog("SERVER", "✓ Server is ONLINE (${ping}ms): ${testedServer.name}", LogLevel.SUCCESS)
            } else {
                _snackMessage.value = "⚠️ تمت إضافة السيرفر ولكن لم يستجب على المنفذ ${serverToAdd.port} (متوقف)"
                TunnelController.addLog("SERVER", "Server unreachable / timeout: ${testedServer.host}:${serverToAdd.port}", LogLevel.WARNING)
            }
        }
    }

    fun selectYouTubeServer() {
        val ytServer = allServers.value.find { it.host.contains("youtube") }
            ?: DefaultServers.list.first()
        selectServer(ytServer)
        _snackMessage.value = "تم تحديد سيرفر YouTube العالمي بنجاح!"
    }

    fun selectCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun selectServer(server: TunnelServer) {
        repository.selectServer(server)
        if (server.rawConfig.isNotBlank()) {
            _targetConfig.value = server.rawConfig
        } else {
            _targetConfig.value = "${server.host}:${server.port}"
        }
        if (server.payload.isNotBlank()) {
            _payloadConfigText.value = server.payload
        }
        TunnelController.addLog("SERVER", "Selected: ${server.name}", LogLevel.INFO)
    }

    fun toggleConnection(context: Context, onPrepareVpn: () -> Unit) {
        val current = connectionState.value
        if (current.isOnline || current.isProgress) {
            TunnelController.stopVpn(context)
        } else {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                _snackMessage.value = "لا يوجد اتصال بالإنترنت في هاتفك! تأكد من تشغيل الواي فاي أو بيانات الهاتف."
                TunnelController.addLog("ERROR", "No active internet connection! Wi-Fi/Data is disabled.", LogLevel.ERROR)
                return
            }

            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) {
                onPrepareVpn()
            } else {
                startVpnDirect(context)
            }
        }
    }

    fun startVpnDirect(context: Context) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            _snackMessage.value = "لا يوجد اتصال بالإنترنت في هاتفك! تأكد من تشغيل الواي فاي أو بيانات الهاتف."
            TunnelController.addLog("ERROR", "No active internet connection! Wi-Fi/Data is disabled.", LogLevel.ERROR)
            return
        }

        val isLocked = _activeConfigFile.value?.isLocked == true
        val target = (if (isLocked) (lockedSecretTarget ?: _activeConfigFile.value?.target ?: "") else _targetConfig.value).trim()
        val proxy = (if (isLocked) (lockedSecretProxy ?: _activeConfigFile.value?.proxy ?: "") else _proxyConfig.value).trim()
        val payload = (if (isLocked) (lockedSecretPayload ?: _activeConfigFile.value?.payload ?: "") else _payloadConfigText.value).trim()

        val currentServer = selectedServer.value
        val effectiveServer = if (target.isNotBlank()) {
            val endpoint = if (target.contains("@")) {
                val p0 = target.substringBefore("@").trim()
                val p1 = target.substringAfter("@").trim()
                val p0Port = p0.substringAfterLast(":").toIntOrNull()
                val p1Port = p1.substringAfterLast(":").toIntOrNull()
                if (p0Port != null && p0Port in 1..65535 && (p1Port == null || !p1.contains("."))) p0
                else if (p1Port != null && p1Port in 1..65535) p1
                else p0
            } else target
            val host = if (endpoint.contains(":")) endpoint.substringBeforeLast(":") else endpoint
            val port = if (endpoint.contains(":")) endpoint.substringAfterLast(":").toIntOrNull() ?: 22 else 22
            currentServer.copy(
                host = host,
                port = port,
                rawConfig = target,
                payload = payload
            )
        } else {
            currentServer
        }

        TunnelController.startVpn(
            context = context,
            server = effectiveServer,
            customTarget = target,
            customProxy = proxy,
            customPayload = payload
        )
    }

    fun testPingForServer(server: TunnelServer) {
        viewModelScope.launch {
            TunnelController.addLog("PING", "Testing latency to ${server.host}:${server.port}...", LogLevel.DEBUG)
            val ping = NetworkUtils.pingHost(server.host, server.port)
            repository.updateServerPing(server.id, ping)
            TunnelController.addLog("PING", "Result for ${server.name}: $ping ms", if (ping < 300) LogLevel.SUCCESS else LogLevel.WARNING)
        }
    }

    fun testAllPings() {
        if (_isTestingPings.value) return
        viewModelScope.launch {
            _isTestingPings.value = true
            TunnelController.addLog("PING", "Initiating parallel latency tests for all nodes...", LogLevel.INFO)
            val currentList = allServers.value
            val deferreds = currentList.map { s ->
                async(Dispatchers.IO) {
                    val ping = NetworkUtils.pingHost(s.host, s.port)
                    s.id to ping
                }
            }
            val results = deferreds.awaitAll()
            results.forEach { (id, ping) ->
                repository.updateServerPing(id, ping)
            }
            _isTestingPings.value = false
            TunnelController.addLog("PING", "✓ Finished pinging all ${currentList.size} servers.", LogLevel.SUCCESS)
        }
    }

    fun updateExtractorInput(text: String) {
        _extractorInput.value = text
    }

    fun extractServerFromInput() {
        val input = _extractorInput.value
        if (input.isBlank()) {
            _snackMessage.value = "Please enter or paste a Dark Tunnel config"
            return
        }
        val result = ServerExtractor.extractConfig(input)
        _extractionResult.value = result
        if (result.success && result.server != null) {
            repository.addServer(result.server)
            _snackMessage.value = "✓ Server extracted and added to list!"
            TunnelController.addLog("EXTRACTOR", "Successfully extracted: ${result.server.name}", LogLevel.SUCCESS)
        } else {
            _snackMessage.value = result.message
            TunnelController.addLog("EXTRACTOR", "Failed to extract: ${result.message}", LogLevel.ERROR)
        }
    }

    fun importCommunityExtractedServer(server: TunnelServer) {
        repository.addServer(server)
        _snackMessage.value = "✓ Added '${server.name}' to server list!"
        TunnelController.addLog("EXTRACTOR", "Imported community node: ${server.name}", LogLevel.SUCCESS)
    }

    fun importAllCommunityServers() {
        ServerExtractor.extractedCommunityFeed.forEach {
            repository.addServer(it)
        }
        _snackMessage.value = "✓ Imported ${ServerExtractor.extractedCommunityFeed.size} community Dark Tunnel servers!"
    }

    fun updateBugHostInput(host: String) {
        _bugHostInput.value = host
    }

    fun checkBugHost() {
        val host = _bugHostInput.value.trim()
        if (host.isEmpty()) return
        viewModelScope.launch {
            _bugHostChecking.value = true
            val res = NetworkUtils.checkBugHost(host)
            _bugHostResult.value = res
            _bugHostChecking.value = false
            if (res.isWorkingBugHost) {
                TunnelController.addLog("BUG_HOST", "✓ $host returned HTTP ${res.statusCode} (${res.statusMessage})", LogLevel.SUCCESS)
            } else {
                TunnelController.addLog("BUG_HOST", "✗ $host failed: ${res.statusMessage}", LogLevel.WARNING)
            }
        }
    }

    fun applyBugHostToCurrentServer(host: String) {
        val cur = selectedServer.value
        val updated = cur.copy(sni = host)
        repository.addServer(updated)
        repository.selectServer(updated)
        _snackMessage.value = "Applied '$host' as SNI Bug Host to ${cur.name}"
        TunnelController.addLog("SNI", "Updated ${cur.name} SNI to: $host", LogLevel.INFO)
    }

    fun updatePayloadBugHost(bugHost: String) {
        val current = payloadConfig.value
        repository.updatePayloadConfig(current.copy(bugHost = bugHost))
    }

    fun updatePayloadInjectionMethod(method: InjectionMethod) {
        val current = payloadConfig.value
        repository.updatePayloadConfig(current.copy(injectionMethod = method))
    }

    fun updatePayloadRequestMethod(method: String) {
        val current = payloadConfig.value
        repository.updatePayloadConfig(current.copy(requestMethod = method))
    }

    fun updatePayloadToggleOnlineHost(enabled: Boolean) {
        val current = payloadConfig.value
        repository.updatePayloadConfig(current.copy(enableOnlineHost = enabled))
    }

    fun updatePayloadToggleKeepAlive(enabled: Boolean) {
        val current = payloadConfig.value
        repository.updatePayloadConfig(current.copy(enableKeepAlive = enabled))
    }

    fun applyPayloadToCurrentServer() {
        val payload = payloadConfig.value.generate()
        val cur = selectedServer.value
        val updated = cur.copy(payload = payload, sni = payloadConfig.value.bugHost)
        repository.addServer(updated)
        repository.selectServer(updated)
        _snackMessage.value = "Applied generated payload to ${cur.name}"
        TunnelController.addLog("PAYLOAD", "Payload applied to active profile", LogLevel.SUCCESS)
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Dark Tunnel") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        _snackMessage.value = "Copied to clipboard!"
    }

    fun clearLogs() {
        TunnelController.clearLogs()
    }

    fun dismissSnack() {
        _snackMessage.value = null
    }

    fun setDns(dns: String) {
        repository.setDnsServer(dns)
        _snackMessage.value = "DNS updated to $dns"
    }

    fun setUdp(enabled: Boolean) {
        repository.setUdpForwarding(enabled)
    }

    private fun loadPublicIp() {
        viewModelScope.launch {
            val ip = NetworkUtils.fetchPublicIp()
            _publicIp.value = ip
        }
    }
}
