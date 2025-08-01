package br.com.fabolearn.ezvizshowcamera.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import br.com.fabolearn.ezvizshowcamera.data.model.WifiNetwork
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings

class WifiRepository(private val context: Context) {

    private val wifiManager: WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager: ConnectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _wifiNetworks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    val wifiNetworks: StateFlow<List<WifiNetwork>> = _wifiNetworks.asStateFlow()

    private val _isWifiEnabled = MutableStateFlow(wifiManager.isWifiEnabled)
    val isWifiEnabled: StateFlow<Boolean> = _isWifiEnabled.asStateFlow()

    private val _currentConnectedWifi = MutableStateFlow<WifiNetwork?>(null)
    val currentConnectedWifi: StateFlow<WifiNetwork?> = _currentConnectedWifi.asStateFlow()

    // Flow para observar mudanças no estado do Wi-Fi e na conectividade
    val wifiStateFlow = callbackFlow<Unit> {
        val wifiStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                        _isWifiEnabled.value = wifiManager.isWifiEnabled
                        if (wifiManager.isWifiEnabled) {
                            scanWifiNetworks() // Escaneia novamente se o Wi-Fi for ligado
                        } else {
                            _wifiNetworks.value = emptyList() // Limpa a lista se o Wi-Fi for desligado
                            _currentConnectedWifi.value = null
                        }
                        trySend(Unit) // Notifica que algo mudou
                    }
                    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> {
                        scanWifiNetworks() // Escaneia quando os resultados da varredura estiverem disponíveis
                        trySend(Unit)
                    }
                    ConnectivityManager.CONNECTIVITY_ACTION -> {
                        updateCurrentConnectedWifi() // Atualiza a rede conectada
                        trySend(Unit)
                    }
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }
        context.registerReceiver(wifiStateReceiver, intentFilter)

        // Limpeza ao cancelar o flow
        awaitClose {
            context.unregisterReceiver(wifiStateReceiver)
        }
    }

    init {
        // Inicia a observação do estado do Wi-Fi e conectividade
        // Isso pode ser coletado no ViewModel para iniciar a varredura
        // Ou, para simplificar, podemos chamar scanWifiNetworks diretamente aqui
        // Mas o callbackFlow é mais robusto para observar mudanças dinâmicas
    }

    fun scanWifiNetworks() {
        if (!wifiManager.isWifiEnabled) {
            _wifiNetworks.value = emptyList()
            _currentConnectedWifi.value = null
            return
        }

        // Verifica se a localização está ativada (necessário para o scan de Wi-Fi no Android 6+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                // Localização desativada, não é possível escanear Wi-Fi
                _wifiNetworks.value = emptyList()
                _currentConnectedWifi.value = null
                return
            }
        }

        wifiManager.startScan() // Inicia o scan
        val scanResults = wifiManager.scanResults
        val currentConnectedSsid = getCurrentConnectedSsid()

        val networks = scanResults.map { scanResult ->
            val security = getSecurityType(scanResult.capabilities)
            WifiNetwork(
                ssid = scanResult.SSID,
                bssid = scanResult.BSSID,
                signalStrength = scanResult.level,
                security = security,
                isCurrent = scanResult.SSID == currentConnectedSsid // Marca a rede atual
            )
        }.sortedByDescending { it.signalStrength } // Ordena por força do sinal

        _wifiNetworks.value = networks
        updateCurrentConnectedWifi() // Garante que a rede conectada seja atualizada após o scan
    }

    private fun getCurrentConnectedSsid(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                return wifiManager.connectionInfo?.ssid?.replace("\"", "") // Remove aspas do SSID
            }
        } else {
            @Suppress("DEPRECATION")
            if (connectivityManager.activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI) {
                @Suppress("DEPRECATION")
                return wifiManager.connectionInfo?.ssid?.replace("\"", "")
            }
        }
        return null
    }

    private fun updateCurrentConnectedWifi() {
        val currentSsid = getCurrentConnectedSsid()
        if (currentSsid != null) {
            // Tenta encontrar a rede conectada na lista de redes escaneadas
            val connectedNetwork = _wifiNetworks.value.find { it.ssid == currentSsid }
            _currentConnectedWifi.value = connectedNetwork?.copy(isCurrent = true) ?: WifiNetwork(
                ssid = currentSsid,
                bssid = wifiManager.connectionInfo?.bssid ?: "N/A",
                signalStrength = wifiManager.connectionInfo?.rssi ?: 0,
                security = "Unknown", // Não podemos determinar a segurança apenas pelo ConnectionInfo
                isCurrent = true
            )
        } else {
            _currentConnectedWifi.value = null
        }
    }

    private fun getSecurityType(capabilities: String): String {
        return when {
            capabilities.contains("WPA3") -> "WPA3"
            capabilities.contains("WPA2") -> "WPA2"
            capabilities.contains("WPA") -> "WPA"
            capabilities.contains("WEP") -> "WEP"
            capabilities.contains("EAP") -> "EAP"
            capabilities.contains("[ESS]") || capabilities.contains("[IBSS]") -> "Open" // Redes abertas
            else -> "Unknown"
        }
    }

    fun openWifiSettings() {
        val panelIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        context.startActivity(panelIntent)
    }

    fun openLocationSettings() {
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }
}