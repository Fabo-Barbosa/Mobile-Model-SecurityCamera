package br.com.fabolearn.ezvizshowcamera.presentation.wifiscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabolearn.ezvizshowcamera.data.model.WifiNetwork
import br.com.fabolearn.ezvizshowcamera.data.repository.WifiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel // <<-- Adicione esta anotação
class WifiScanViewModel @Inject constructor( // <<-- Certifique-se de que o construtor tem @Inject
    private val wifiRepository: WifiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WifiScanUiState())
    val uiState: StateFlow<WifiScanUiState> = _uiState.asStateFlow()
    val _wifiRepository: WifiRepository = wifiRepository

    init {
        // Coleta os fluxos do repositório para atualizar o estado da UI
        viewModelScope.launch {
            wifiRepository.wifiNetworks.collectLatest { networks ->
                _uiState.value = _uiState.value.copy(availableNetworks = networks)
            }
        }
        viewModelScope.launch {
            wifiRepository.isWifiEnabled.collectLatest { isEnabled ->
                _uiState.value = _uiState.value.copy(isWifiEnabled = isEnabled)
            }
        }
        viewModelScope.launch {
            wifiRepository.currentConnectedWifi.collectLatest { connectedWifi ->
                _uiState.value = _uiState.value.copy(currentConnectedNetwork = connectedWifi)
                if (connectedWifi != null && _uiState.value.selectedNetwork == null) {
                    // Seleciona automaticamente a rede conectada se nenhuma outra estiver selecionada
                    _uiState.value = _uiState.value.copy(selectedNetwork = connectedWifi)
                }
            }
        }
        // Inicia a observação de mudanças no estado do Wi-Fi (via Broadcast Receiver)
        viewModelScope.launch {
            wifiRepository.wifiStateFlow.collectLatest {
                // Quando o estado do Wi-Fi mudar, ou novos resultados de scan estiverem disponíveis,
                // o repositório já atualiza seus fluxos. Basta coletar aqui.
                // Isso garante que a UI seja reativa a essas mudanças.
            }
        }
    }

    fun scanNetworks() {
        wifiRepository.scanWifiNetworks()
    }

    fun onNetworkSelected(network: WifiNetwork) {
        _uiState.value = _uiState.value.copy(selectedNetwork = network)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun connectToSelectedNetwork() {
        val selected = _uiState.value.selectedNetwork
        val password = _uiState.value.password

        if (selected != null) {
            // Aqui você chamaria a função da câmera.
            // Por exemplo: cameraService.configureWifi(selected.ssid, password)
            // Lembre-se que essa função de câmera deve ser injetada ou acessível aqui.
            println("Tentando configurar câmera com SSID: ${selected.ssid} e senha: $password")
            _uiState.value = _uiState.value.copy(
                connectionStatusMessage = "Tentando conectar a ${selected.ssid}...",
                showConnectionStatus = true
            )
            // TODO: Implementar a chamada real para a câmera e lidar com o resultado
            // Por exemplo, usando corrotinas para uma operação assíncrona
            viewModelScope.launch {
                try {
                    // Simula uma operação de conexão
                    kotlinx.coroutines.delay(2000)
                    val success = true // Simule sucesso/falha da conexão com a câmera
                    if (success) {
                        _uiState.value = _uiState.value.copy(
                            connectionStatusMessage = "Conectado com sucesso a ${selected.ssid}!",
                            showConnectionStatus = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            connectionStatusMessage = "Falha ao conectar a ${selected.ssid}.",
                            showConnectionStatus = true
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        connectionStatusMessage = "Erro: ${e.localizedMessage}",
                        showConnectionStatus = true
                    )
                }
            }
        }
    }

    fun dismissConnectionStatus() {
        _uiState.value = _uiState.value.copy(showConnectionStatus = false, connectionStatusMessage = null)
    }

    fun openWifiSettings() {
        wifiRepository.openWifiSettings()
    }

    fun openLocationSettings() {
        wifiRepository.openLocationSettings()
    }
}

// Representa o estado da UI
data class WifiScanUiState(
    val availableNetworks: List<WifiNetwork> = emptyList(),
    val isWifiEnabled: Boolean = false,
    val isLocationEnabled: Boolean = true, // Você precisaria verificar isso também
    val currentConnectedNetwork: WifiNetwork? = null,
    val selectedNetwork: WifiNetwork? = null,
    val password: String = "",
    val connectionStatusMessage: String? = null,
    val showConnectionStatus: Boolean = false
)