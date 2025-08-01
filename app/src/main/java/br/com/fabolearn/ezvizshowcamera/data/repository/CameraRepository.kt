package br.com.fabolearn.ezvizshowcamera.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import br.com.fabolearn.ezvizshowcamera.data.model.Camera
import br.com.fabolearn.ezvizshowcamera.data.model.QRCodeInfo
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraRepository @Inject constructor() {
    private val _cameras = MutableStateFlow(
        listOf(
            Camera("cam_001", "Sala", "Serial", "codigo", "Ezviz"),
            Camera("cam_002", "Cozinha", "Serial", "codigo", "Ezviz")
        )
    )

    val cameras: StateFlow<List<Camera>> = _cameras.asStateFlow()

    fun addCamera(camera: Camera) {
        val currentList = _cameras.value.toMutableList()
        if (!currentList.any { it.serial == camera.serial && it.code == camera.code }) {
            currentList.add(camera)
            _cameras.value = currentList
        }else {
            println("Câmera com serial ${camera.serial} e código ${camera.code} já existe.")
            // Você pode adicionar uma lógica para notificar a UI sobre isso
        }
    }

    // Adiciona câmera a partir de QRCodeInfo
    fun addCameraFromQr(qrInfo: QRCodeInfo) {
        val newCamera = Camera(
            id = qrInfo.serial, // Usando serial como ID único
            name = qrInfo.serial, // Nome inicial é o serial
            serial = qrInfo.serial,
            code = qrInfo.code,
            brand = qrInfo.brand
        )
        addCamera(newCamera)
    }

    fun updateCameraName(id: String, newName: String) {
        val currentList = _cameras.value.toMutableList()
        val index = currentList.indexOfFirst {
            it.id == id
        }

        if (index != -1) {
            currentList[index] = currentList[index].copy(name = newName)
            _cameras.value = currentList
        }
    }

    fun updateCameraCodigo(id: String, newCodigo: String) {
        val currentList = _cameras.value.toMutableList()
        val index = currentList.indexOfFirst {
            it.id == id
        }

        if (index != -1) {
            currentList[index] = currentList[index].copy(code = newCodigo)
            _cameras.value = currentList
        }
    }

    fun getCameraById(id: String) : Camera? {
        return _cameras.value.find { it.id == id }
    }

    // Verifica se uma câmera com o dado serial e código já existe
    fun doesCameraExist(serial: String, code: String): Boolean {
        return _cameras.value.any { it.serial == serial && it.code == code }
    }
}