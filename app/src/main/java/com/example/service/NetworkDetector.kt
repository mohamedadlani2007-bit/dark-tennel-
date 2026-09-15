package com.example.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import java.net.Inet4Address
import java.net.NetworkInterface

data class NetworkInfo(
    val typeName: String,
    val carrierName: String,
    val isOnline: Boolean,
    val localIp: String,
    val recommendedBugHost: String,
    val recommendedOfferName: String,
    val offerDescription: String
)

object NetworkDetector {

    fun detect(context: Context): NetworkInfo {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }

        val isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        if (!isOnline) {
            return NetworkInfo(
                typeName = "غير متصل (Offline)",
                carrierName = "لا توجد شبكة",
                isOnline = false,
                localIp = "127.0.0.1",
                recommendedBugHost = "youtube.com",
                recommendedOfferName = "تنبيه",
                offerDescription = "يرجى تشغيل الواي فاي أو بيانات الهاتف لبدء الاتصال."
            )
        }

        // Check if Wi-Fi
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val localIp = getLocalIpAddress()
            return NetworkInfo(
                typeName = "Wi-Fi شبكة لاسلكية",
                carrierName = "واي فاي منزلي / ألياف بصرية",
                isOnline = true,
                localIp = localIp,
                recommendedBugHost = "youtube.com",
                recommendedOfferName = "Google & Anycast CDN",
                offerDescription = "أفضل استجابة وسرعة تنزيل فائقة عبر خوادم Google/YouTube و Cloudflare 100G."
            )
        }

        // Cellular / Mobile Data
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        var rawOperator = tm?.networkOperatorName?.trim().orEmpty()
        if (rawOperator.isEmpty()) {
            rawOperator = tm?.simOperatorName?.trim().orEmpty()
        }

        val operatorLower = rawOperator.lowercase()
        val localIp = getLocalIpAddress()

        return when {
            operatorLower.contains("mobilis") || operatorLower.contains("60301") -> {
                NetworkInfo(
                    typeName = "بيانات الهاتف 4G LTE",
                    carrierName = "Mobilis مـوبـيـلـيـس",
                    isOnline = true,
                    localIp = localIp,
                    recommendedBugHost = "wap.mobilis.dz",
                    recommendedOfferName = "عـرض PixX / Navigui Zero",
                    offerDescription = "باستخدام ثغرة الـ SNI الخاصة بموبيليس wap.mobilis.dz أو youtube.com للتخطي."
                )
            }
            operatorLower.contains("djezzy") || operatorLower.contains("ota") || operatorLower.contains("60302") -> {
                NetworkInfo(
                    typeName = "بيانات الهاتف 4G LTE",
                    carrierName = "Djezzy جـازي",
                    isOnline = true,
                    localIp = localIp,
                    recommendedBugHost = "internet.djezzy.dz",
                    recommendedOfferName = "عـرض هـايلة Haya & Special",
                    offerDescription = "باستخدام منفذ WebSocket وسيرفر جازي internet.djezzy.dz لتخطي الرصيد."
                )
            }
            operatorLower.contains("ooredoo") || operatorLower.contains("nedjma") || operatorLower.contains("60303") -> {
                NetworkInfo(
                    typeName = "بيانات الهاتف 4G LTE",
                    carrierName = "Ooredoo أوريـدو",
                    isOnline = true,
                    localIp = localIp,
                    recommendedBugHost = "ooredoo.dz",
                    recommendedOfferName = "عـروض La Gold & Yassir",
                    offerDescription = "استخدم سيرفر youtube.com أو ooredoo.dz للحصول على إنترنت سريع ومستقر."
                )
            }
            else -> {
                val displayCarrier = if (rawOperator.isNotEmpty()) rawOperator else "شريحة اتصال متنقلة"
                NetworkInfo(
                    typeName = "بيانات الهاتف 4G LTE",
                    carrierName = displayCarrier,
                    isOnline = true,
                    localIp = localIp,
                    recommendedBugHost = "youtube.com",
                    recommendedOfferName = "عرض الشبكة العامة",
                    offerDescription = "سيرفر youtube.com متوافق مع كافة الشبكات العالمية والمحلية دون حظر."
                )
            }
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "10.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}
        return "10.0.2.15"
    }
}
