package br.com.fabolearn.ezvizshowcamera.presentation.cameralist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fabolearn.ezvizshowcamera.data.model.Camera
import br.com.fabolearn.ezvizshowcamera.data.model.QRCodeInfo
import br.com.fabolearn.ezvizshowcamera.data.repository.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraListViewModel @Inject constructor(
    private val cameraRepository: CameraRepository
): ViewModel() {
    private val _cameras = MutableStateFlow<List<Camera>>(emptyList())
    val cameras : StateFlow<List<Camera>> = _cameras.asStateFlow()

    // Estado para a nova câmera a ser adicionada via digitação (serial + code)
    private val _newCameraSerialForWifi = MutableStateFlow<String?>(null)
    val newCameraSerialForWifi: StateFlow<String?> = _newCameraSerialForWifi.asStateFlow()

    init {
        viewModelScope.launch {
            cameraRepository.cameras.collectLatest {
                _cameras.value = it
            }
        }
        // Em um app real, aqui você carregaria as câmeras do DB/API
        // cameraRepository.loadCameras()
    }

    fun renameCamera(camera: Camera, newName: String) {
        cameraRepository.updateCameraName(camera.id, newName)
    }

    // Placeholder para as ações do menu
    fun onAddWifiClicked(camera: Camera) {
        // Redireciona para o fluxo manual de Wi-Fi, agora com uma etapa extra (código)
        _eventFlow.value = CameraListEvent.PromptForCameraCodeBeforeWifi(camera.serial)
    }

    fun onRenameClicked(camera: Camera) {
        // Lógica para assistir ao vivo
        _eventFlow.value = CameraListEvent.ShowRenameDialog(camera)
    }

    fun onWatchLiveClicked(camera: Camera) {
        // Lógica para assistir ao vivo
        _eventFlow.value = CameraListEvent.ShowMessage("Assistir ao vivo: ${camera.name}")
    }

    fun onWatchRecordingsClicked(camera: Camera) {
        // Lógica para assistir gravações
        _eventFlow.value = CameraListEvent.ShowMessage("Assistir gravações: ${camera.name}")
    }

    // Ação para iniciar a leitura do QR Code
    fun onScanQrCodeClicked() {
        _eventFlow.value = CameraListEvent.StartQrCodeScan
    }

    // Processar resultado do QR Code
    fun processQrCodeResult(qrInfo: QRCodeInfo) {
        viewModelScope.launch {
            // Adiciona a câmera automaticamente se não existir
            if (!cameraRepository.doesCameraExist(qrInfo.serial, qrInfo.code)) {
                cameraRepository.addCameraFromQr(qrInfo)
                _eventFlow.value = CameraListEvent.ShowMessage("Câmera ${qrInfo.serial} adicionada e pronta para configurar Wi-Fi.")
            } else {
                _eventFlow.value = CameraListEvent.ShowMessage("Câmera ${qrInfo.serial} já existe. Abrindo tela de Wi-Fi.")
            }
            // Redireciona para a tela de Wi-Fi Scan com o serial
            _eventFlow.value = CameraListEvent.NavigateToWifiScan(qrInfo.serial)
        }
    }

    // Valida o código da câmera antes de navegar para WifiScanActivity
    fun validateCameraCodeAndNavigateToWifi(serial: String, code: String) {
        viewModelScope.launch {
            if (cameraRepository.doesCameraExist(serial, code)) {
                _eventFlow.value = CameraListEvent.NavigateToWifiScan(serial)
            } else {
                _eventFlow.value = CameraListEvent.ShowMessage("Câmera com Serial: $serial e Código: $code não encontrada. Por favor, verifique ou adicione a câmera primeiro.")
            }
        }
    }

    // --- Events para a Activity ---
    private val _eventFlow = MutableStateFlow<CameraListEvent?>(null)
    val eventFlow: StateFlow<CameraListEvent?> = _eventFlow.asStateFlow()

    fun consumeEvent() {
        _eventFlow.value = null // Limpa o evento após ser consumido pela Activity
    }
}

// Classe selada para representar os diferentes eventos que o ViewModel pode enviar para a Activity
sealed class CameraListEvent {
    data class NavigateToWifiScan(val cameraSerial: String) : CameraListEvent()
    data class ShowRenameDialog(val camera: Camera) : CameraListEvent()
    data class ShowMessage(val message: String) : CameraListEvent()
    data object StartQrCodeScan : CameraListEvent() // Novo evento para iniciar o scan
    data class PromptForCameraCodeBeforeWifi(val cameraSerial: String) : CameraListEvent() // Novo evento para pedir o código
}