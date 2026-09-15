package com.example.model

data class PayloadConfig(
    val bugHost: String = "m.youtube.com",
    val requestMethod: String = "CONNECT",
    val injectionMethod: InjectionMethod = InjectionMethod.NORMAL,
    val splitMethod: SplitMethod = SplitMethod.NONE,
    val enableKeepAlive: Boolean = true,
    val enableOnlineHost: Boolean = true,
    val enableForwardHost: Boolean = false,
    val enableUserAgent: Boolean = true,
    val customSni: String = "speedtest.net",
    val customDns: String = "1.1.1.1"
) {
    fun generate(): String {
        val crlf = "[crlf]"
        val sb = StringBuilder()

        when (injectionMethod) {
            InjectionMethod.NORMAL -> {
                sb.append("$requestMethod [host_port] [protocol]$crlf")
            }
            InjectionMethod.FRONT -> {
                sb.append("GET http://$bugHost/ HTTP/1.1$crlf")
                sb.append("Host: $bugHost$crlf$crlf")
                sb.append("$requestMethod [host_port] [protocol]$crlf")
            }
            InjectionMethod.BACK -> {
                sb.append("$requestMethod [host_port] [protocol]$crlf")
                sb.append("Host: $bugHost$crlf")
                sb.append("X-Online-Host: $bugHost$crlf")
            }
            InjectionMethod.WEBSOCKET -> {
                sb.append("GET / HTTP/1.1$crlf")
                sb.append("Host: [host]$crlf")
                sb.append("Upgrade: websocket$crlf")
                sb.append("Connection: Upgrade$crlf")
                sb.append("User-Agent: [ua]$crlf$crlf")
                return sb.toString()
            }
        }

        if (enableOnlineHost) {
            sb.append("Host: $bugHost$crlf")
            sb.append("X-Online-Host: $bugHost$crlf")
        }
        if (enableForwardHost) {
            sb.append("X-Forward-Host: $bugHost$crlf")
        }
        if (enableKeepAlive) {
            sb.append("Connection: Keep-Alive$crlf")
        }
        if (enableUserAgent) {
            sb.append("User-Agent: [ua]$crlf")
        }
        sb.append(crlf)

        return sb.toString()
    }
}

enum class InjectionMethod(val title: String) {
    NORMAL("Normal Inject"),
    FRONT("Front Inject"),
    BACK("Back Inject"),
    WEBSOCKET("WebSocket CDN")
}

enum class SplitMethod(val title: String) {
    NONE("No Split"),
    NORMAL("Split [Instant]"),
    DELAY("Split [Delay]")
}
