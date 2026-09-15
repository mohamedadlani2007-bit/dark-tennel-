package com.example.model

enum class ConnectionState(val title: String) {
    DISCONNECTED("DISCONNECTED"),
    CONNECTING("CONNECTING..."),
    AUTHENTICATING("AUTHENTICATING..."),
    CONNECTED("CONNECTED"),
    RECONNECTING("RECONNECTING..."),
    ERROR("FAILED");

    val isOnline: Boolean
        get() = this == CONNECTED

    val isProgress: Boolean
        get() = this == CONNECTING || this == AUTHENTICATING || this == RECONNECTING
}
