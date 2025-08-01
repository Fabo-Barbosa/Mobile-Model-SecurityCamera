package br.com.fabolearn.ezvizshowcamera.data.model

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val signalStrength: Int,
    val security: String,
    val isCurrent: Boolean
)

