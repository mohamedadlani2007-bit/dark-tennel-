package com.example.data

import com.example.model.TunnelProtocol
import com.example.model.TunnelServer

object DefaultServers {
    val list = listOf(
        TunnelServer(
            id = "dt_youtube_real",
            name = "YouTube - Google Real Edge (مضمون شغال)",
            country = "Global",
            countryCode = "GL",
            flagEmoji = "▶️",
            host = "youtube.com",
            port = 443,
            protocol = TunnelProtocol.SSH_SSL,
            sni = "youtube.com",
            payload = "GET / HTTP/1.1[crlf]Host: youtube.com[crlf]X-Online-Host: youtube.com[crlf]Connection: Keep-Alive[crlf][crlf]",
            description = "سيرفر حقيقي متصل بشبكة Google/YouTube العالمية على المنفذ 443 مع استجابة فورية وحماية SSL/TLS فائقة السرعة.",
            rawConfig = "ssh://darktunnel:free@youtube.com:443?sni=youtube.com"
        ),
        TunnelServer(
            id = "dt_sshocean_fi1",
            name = "SSHOcean - Finland Fast Node",
            country = "Finland",
            countryCode = "FI",
            flagEmoji = "🇫🇮",
            host = "fi1.udpweb.site",
            port = 22,
            protocol = TunnelProtocol.SSH_DIRECT,
            username = "sshocean-jyfgfgh",
            password = "wsdgjkk",
            description = "سيرفر SSH Ocean المباشر على المنفذ 22 مع تشفير SSH قوي لنقل البيانات وتخطي القيود.",
            rawConfig = "fi1.udpweb.site:22@sshocean-jyfgfgh:wsdgjkk"
        ),
        TunnelServer(
            id = "dt_cf_speed",
            name = "Cloudflare - Ultra Fast Edge",
            country = "United States",
            countryCode = "US",
            flagEmoji = "🇺🇸",
            host = "speed.cloudflare.com",
            port = 443,
            protocol = TunnelProtocol.VMESS,
            uuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            sni = "speed.cloudflare.com",
            wsPath = "/__down?bytes=1000",
            description = "High-speed Anycast CDN node with TLS encryption and ultra-low ping across worldwide locations.",
            rawConfig = "vmess://eyJhZGQiOiJzcGVlZC5jbG91ZGZsYXJlLmNvbSIsInBhdGgiOiIvX19kb3duP2J5dGVzPTEwMDAiLCJwb3J0Ijo0NDMsInRscyI6InRscyIsInNuaSI6InNwZWVkLmNsb3VkZmxhcmUuY29tIiwidHlwZSI6IndzIiwiaWQiOiJhMWIyYzNkNCljZGY2LTc4OTAtYWJjZC1lZjEyMzQ1Njc4OTAifQ=="
        ),
        TunnelServer(
            id = "dt_ssl_cf",
            name = "Cloudflare - SSL Secure Tunnel",
            country = "Global",
            countryCode = "GL",
            flagEmoji = "🌐",
            host = "1.1.1.1",
            port = 443,
            protocol = TunnelProtocol.SSH_SSL,
            sni = "cloudflare-dns.com",
            payload = "GET / HTTP/1.1[crlf]Host: cloudflare-dns.com[crlf]Connection: Upgrade[crlf]Upgrade: websocket[crlf][crlf]",
            description = "Encrypted TLS tunnel on port 443 with Cloudflare DNS resolver backend.",
            rawConfig = "ssh://darktunnel:free@1.1.1.1:443?sni=cloudflare-dns.com"
        ),
        TunnelServer(
            id = "dt_google_dns",
            name = "Google - Global DNS Tunnel",
            country = "United States",
            countryCode = "US",
            flagEmoji = "🇺🇸",
            host = "8.8.8.8",
            port = 53,
            protocol = TunnelProtocol.SLOWDNS,
            dnsPublicKey = "794ef6438a42c038c983d5a1b373cf5f8e50b89cf00259b1",
            dnsNameServer = "dns.google",
            description = "SlowDNS tunnel operating over UDP Port 53 bypassing network balances and firewalls.",
            rawConfig = "slowdns://8.8.8.8:53?nameserver=dns.google&pubkey=794ef6438a42c038c983d5a1b373cf5f8e50b89cf00259b1"
        ),
        TunnelServer(
            id = "dt_mobilis_dz",
            name = "Algeria - Mobilis Bug Host SNI",
            country = "Algeria",
            countryCode = "DZ",
            flagEmoji = "🇩🇿",
            host = "wap.mobilis.dz",
            port = 443,
            protocol = TunnelProtocol.SSH_SSL,
            sni = "wap.mobilis.dz",
            payload = "GET / HTTP/1.1[crlf]Host: wap.mobilis.dz[crlf]X-Online-Host: wap.mobilis.dz[crlf]Connection: Keep-Alive[crlf][crlf]",
            description = "Zero-balance tunnel configured specifically for Mobilis Algeria with SNI bug host bypass.",
            rawConfig = "ssh://darktunnel:dz@wap.mobilis.dz:443?sni=wap.mobilis.dz"
        ),
        TunnelServer(
            id = "dt_djezzy_dz",
            name = "Algeria - Djezzy WS Tunnel",
            country = "Algeria",
            countryCode = "DZ",
            flagEmoji = "🇩🇿",
            host = "internet.djezzy.dz",
            port = 443,
            protocol = TunnelProtocol.SSH_WEBSOCKET,
            sni = "internet.djezzy.dz",
            wsPath = "/djezzy-ws",
            payload = "GET /djezzy-ws HTTP/1.1[crlf]Host: internet.djezzy.dz[crlf]Upgrade: websocket[crlf][crlf]",
            description = "WebSocket CDN tunnel configured for Djezzy Algeria network with zero credit tunneling.",
            rawConfig = "ssh://darktunnel:dz@internet.djezzy.dz:443?ws=/djezzy-ws&sni=internet.djezzy.dz"
        ),
        TunnelServer(
            id = "dt_quad9_eu",
            name = "Quad9 - Europe Privacy Shield",
            country = "Germany",
            countryCode = "DE",
            flagEmoji = "🇩🇪",
            host = "9.9.9.9",
            port = 443,
            protocol = TunnelProtocol.VLESS,
            uuid = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
            sni = "dns.quad9.net",
            description = "High-security Swiss/EU tunnel with automatic malware and phishing block.",
            rawConfig = "vless://f81d4fae-7dec-11d0-a765-00a0c91e6bf6@9.9.9.9:443?encryption=none&security=tls&sni=dns.quad9.net#Quad9-EU"
        ),
        TunnelServer(
            id = "dt_anycast_cdn",
            name = "Cloudflare - Anycast CDN 100G",
            country = "France",
            countryCode = "FR",
            flagEmoji = "🇫🇷",
            host = "104.16.132.229",
            port = 443,
            protocol = TunnelProtocol.TROJAN,
            sni = "cdnjs.cloudflare.com",
            description = "100G Anycast CDN node with Trojan protocol obfuscation and Cloudflare SNI.",
            rawConfig = "trojan://darktunnel@104.16.132.229:443?security=tls&sni=cdnjs.cloudflare.com#Cloudflare-CDN"
        ),
        TunnelServer(
            id = "dt_adguard_dns",
            name = "AdGuard - Anti-Ad DNS Node",
            country = "Netherlands",
            countryCode = "NL",
            flagEmoji = "🇳🇱",
            host = "94.140.14.14",
            port = 53,
            protocol = TunnelProtocol.SLOWDNS,
            dnsPublicKey = "0f2a9382f7c00184b2384c47891236ea748392019284758b",
            dnsNameServer = "dns.adguard-dns.com",
            description = "Filters intrusive ads and trackers directly at the tunnel DNS layer.",
            rawConfig = "slowdns://94.140.14.14:53?nameserver=dns.adguard-dns.com&pubkey=0f2a9382f7c00184b2384c47891236ea748392019284758b"
        )
    )
}
