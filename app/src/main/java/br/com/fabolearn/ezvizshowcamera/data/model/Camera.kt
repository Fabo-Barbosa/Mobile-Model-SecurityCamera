package br.com.fabolearn.ezvizshowcamera.data.model

data class Camera(
    val id: String, // Um ID único para a câmera, pode ser o serial
    var name: String, // Nome da câmera (inicialmente o serial)
    val serial: String, // Número de série da câmera
    val code: String = "NOCODE",
    val brand: String
    // val imageUrl: String? = null, // URL da imagem da câmera (se tiver)
    // val isOnline: Boolean = false // Status online/offline
)

data class QRCodeInfo(
    val brand: String,
    val serial:  String,
    val code: String,
    val model: String
)
