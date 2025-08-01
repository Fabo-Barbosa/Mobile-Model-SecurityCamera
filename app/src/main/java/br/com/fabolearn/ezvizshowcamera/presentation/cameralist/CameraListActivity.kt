// app/src/main/java/com/seunomeapp/presentation/cameralist/CameraListActivity.kt
package br.com.fabolearn.ezvizshowcamera.presentation.cameralist

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import br.com.fabolearn.ezvizshowcamera.BuildConfig
import br.com.fabolearn.ezvizshowcamera.R
import br.com.fabolearn.ezvizshowcamera.data.model.Camera
import br.com.fabolearn.ezvizshowcamera.data.model.QRCodeInfo
import br.com.fabolearn.ezvizshowcamera.databinding.ActivityCameraListBinding
import dagger.hilt.android.AndroidEntryPoint
import br.com.fabolearn.ezvizshowcamera.presentation.wifiscan.WifiScanActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.videogo.openapi.EZGlobalSDK
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CameraListActivity : AppCompatActivity() {

    private val APP_KEY = BuildConfig.APP_KEY

    private lateinit var binding: ActivityCameraListBinding
    private val viewModel: CameraListViewModel by viewModels()
    private lateinit var cameraAdapter: CameraAdapter

    // Launcher para permissão de câmera (para QR Code)
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startQrCodeScanner()
            } else {
                Toast.makeText(this, "Permissão de câmera é necessária para escanear QR Code.", Toast.LENGTH_LONG).show()
            }
        }

    // Launcher para o resultado do QR Code
    private val qrCodeScannerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val intentResult = IntentIntegrator.parseActivityResult(result.resultCode, result.data)
        if (intentResult != null && intentResult.contents != null) {
            // QR Code lido com sucesso
            val qrContent = intentResult.contents
            parseAndProcessQrCode(qrContent)
        } else {
            Toast.makeText(this, "Leitura do QR Code cancelada ou falhou.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar) // Configura a Toolbar
        supportActionBar?.title = "Suas Câmeras" // Define o título

        setupRecyclerView()
        setupListeners() // Novo método para listeners dos botões da Toolbar
        observeViewModel()

        loadThemeSetting() // Carrega o tema ao iniciar

        EZGlobalSDK.showSDKLog(true)
        EZGlobalSDK.enableP2P(false)
        EZGlobalSDK.initLib(this.application, APP_KEY)
    }

    private fun setupRecyclerView() {
        cameraAdapter = CameraAdapter { camera, menuItemType ->
            when (menuItemType) {
                MenuItemType.ADD_WIFI -> viewModel.onAddWifiClicked(camera)
                MenuItemType.RENAME -> viewModel.onRenameClicked(camera)
                MenuItemType.WATCH_LIVE -> viewModel.onWatchLiveClicked(camera)
                MenuItemType.WATCH_RECORDINGS -> viewModel.onWatchRecordingsClicked(camera)
            }
        }
        binding.recyclerViewCameras.apply {
            layoutManager = GridLayoutManager(this@CameraListActivity, 2) // 2 colunas
            adapter = cameraAdapter
        }
    }

    private fun setupListeners() {
        binding.btnScanQr.setOnClickListener {
            checkCameraPermissionAndScanQr()
        }

        binding.btnToggleTheme.setOnClickListener {
            toggleAppTheme()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.cameras.collectLatest { cameras ->
                cameraAdapter.submitList(cameras)
            }
        }

        lifecycleScope.launch {
            viewModel.eventFlow.collectLatest { event ->
                event?.let {
                    when (it) {
                        is CameraListEvent.NavigateToWifiScan -> {
                            val intent = Intent(this@CameraListActivity, WifiScanActivity::class.java)
                            intent.putExtra("CAMERA_SERIAL", it.cameraSerial)
                            startActivity(intent)
                        }
                        is CameraListEvent.ShowRenameDialog -> {
                            showRenameDialog(it.camera)
                        }
                        is CameraListEvent.ShowMessage -> {
                            Toast.makeText(this@CameraListActivity, it.message, Toast.LENGTH_SHORT).show()
                        }
                        is CameraListEvent.StartQrCodeScan -> {
                            startQrCodeScanner()
                        }
                        is CameraListEvent.PromptForCameraCodeBeforeWifi -> {
                            showEnterCameraCodeDialog(it.cameraSerial)
                        }
                    }
                    viewModel.consumeEvent()
                }
            }
        }
    }

    // --- Lógica de QR Code ---
    private fun checkCameraPermissionAndScanQr() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startQrCodeScanner()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startQrCodeScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setPrompt("Aponte para o QR Code da câmera")
        integrator.setOrientationLocked(false) // Permite rotação
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setBeepEnabled(false)
        integrator.setOrientationLocked(true)
        integrator.initiateScan()
        // O resultado será tratado no qrCodeScannerLauncher
    }

    private fun parseAndProcessQrCode(qrContent: String) {
        // Exemplo de parse: "Ezviz\nABC123XYZ001\nC001\nC6CN"
        val parts = qrContent.split("\n")
        if (parts.size == 4) {
            val qrInfo = QRCodeInfo(
                brand = parts[0],
                serial = parts[1],
                code = parts[2],
                model = parts[3]
            )
            viewModel.processQrCodeResult(qrInfo)
        } else {
            Toast.makeText(this, "Formato de QR Code inválido.", Toast.LENGTH_LONG).show()
        }
    }

    // --- Lógica de Adicionar Wi-Fi (Etapa Extra) --- // mudança utilizando
    private fun showEnterCameraCodeDialog(cameraSerial: String) {
        val input = EditText(this)
        input.setSingleLine(true)
        input.hint = "Digite o código da câmera"

        AlertDialog.Builder(this)
            .setTitle("Adicionar Wi-Fi")
            .setMessage("Para configurar o Wi-Fi de ${cameraSerial}, digite o código da câmera:")
            .setView(input)
            .setPositiveButton("Continuar") { dialog, _ ->
                val cameraCode = input.text.toString().trim()
                if (cameraCode.isNotBlank()) {
                    viewModel.validateCameraCodeAndNavigateToWifi(cameraSerial, cameraCode)
                } else {
                    Toast.makeText(this, "O código da câmera não pode ser vazio.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    // --- Lógica de Renomear Câmera ---
    private fun showRenameDialog(camera: Camera) {
        val input = EditText(this)
        input.setText(camera.name)
        input.setSingleLine(true)

        AlertDialog.Builder(this)
            .setTitle("Renomear Câmera")
            .setMessage("Digite o novo nome para ${camera.serial}:")
            .setView(input)
            .setPositiveButton("Renomear") { dialog, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotBlank()) {
                    viewModel.renameCamera(camera, newName)
                } else {
                    Toast.makeText(this, "O nome não pode ser vazio.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    // --- Lógica de Troca de Tema ---
    private fun toggleAppTheme() {
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        val newNightMode = if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.MODE_NIGHT_NO // Mudar para light
        } else {
            AppCompatDelegate.MODE_NIGHT_YES // Mudar para dark
        }
        AppCompatDelegate.setDefaultNightMode(newNightMode)
        saveThemeSetting(newNightMode) // Salva a preferência
        updateThemeToggleButtonIcon(newNightMode) // Atualiza o ícone
        recreate() // Recria a Activity para aplicar o novo tema
    }

    private fun loadThemeSetting() {
        val sharedPrefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val savedTheme = sharedPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedTheme)
        updateThemeToggleButtonIcon(savedTheme)
    }

    private fun saveThemeSetting(mode: Int) {
        val sharedPrefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("theme_mode", mode).apply()
    }

    private fun updateThemeToggleButtonIcon(currentMode: Int) {
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            binding.btnToggleTheme.setImageResource(R.drawable.outline_dark_mode_24) // Ícone para tema escuro (lua)
        } else {
            binding.btnToggleTheme.setImageResource(R.drawable.outline_light_mode_24) // Ícone para tema claro (sol)
        }
    }
}