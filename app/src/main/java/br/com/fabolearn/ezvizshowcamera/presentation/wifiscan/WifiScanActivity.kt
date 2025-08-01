package br.com.fabolearn.ezvizshowcamera.presentation.wifiscan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import android.location.LocationManager // Necessário para verificar estado da localização
import androidx.core.widget.addTextChangedListener
import br.com.fabolearn.ezvizshowcamera.R
import br.com.fabolearn.ezvizshowcamera.databinding.ActivityWifiScanBinding

@AndroidEntryPoint
class WifiScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWifiScanBinding
    private val viewModel: WifiScanViewModel by viewModels()
    private lateinit var wifiAdapter: WifiNetworkAdapter

    private val requestPermissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val arePermissionsGranted = permissions.entries.all { it.value }

            if (arePermissionsGranted) {
                viewModel.scanNetworks()
            } else {
                Snackbar.make(
                    binding.root,
                    "Permissões de localização e/ou Wi-Fi negadas. Não é possível escanear redes.",
                    Snackbar.LENGTH_LONG
                ).show()
                // Opcional: Atualizar um estado no ViewModel para que a UI reflita a permissão negada
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Infla o layout usando View Binding
        binding = ActivityWifiScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura a Toolbar
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_refresh -> {
                    checkAndRequestPermissions()
                    true
                }
                else -> false
            }
        }

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        // Inicia a verificação de permissões e scan ao criar a Activity
        checkAndRequestPermissions()
    }

    private fun setupRecyclerView() {
        wifiAdapter = WifiNetworkAdapter(wifiRepository = viewModel._wifiRepository) { network ->
            viewModel.onNetworkSelected(network)
            // Fecha o teclado virtual quando uma rede é selecionada
            // Hide the keyboard after selecting a network (optional)
            // val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            // imm.hideSoftInputFromWindow(binding.etPassword.windowToken, 0)
        }
        binding.recyclerViewWifiNetworks.apply {
            layoutManager = LinearLayoutManager(this@WifiScanActivity)
            adapter = wifiAdapter
        }
    }

    private fun setupListeners() {
        binding.btnOpenWifiSettings.setOnClickListener {
            viewModel.openWifiSettings()
        }
        binding.btnOpenLocationSettings.setOnClickListener {
            viewModel.openLocationSettings()
        }
        binding.etPassword.addTextChangedListener { editable ->
            viewModel.onPasswordChanged(editable.toString())
        }
        binding.btnConnect.setOnClickListener {
            viewModel.connectToSelectedNetwork()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { uiState ->
                updateUi(uiState)
            }
        }

        // Observa mensagens de status de conexão
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { uiState ->
                if (uiState.showConnectionStatus && uiState.connectionStatusMessage != null) {
                    Snackbar.make(binding.root, uiState.connectionStatusMessage, Snackbar.LENGTH_SHORT).show()
                    // Disparar um evento para o ViewModel esconder o Snackbar após mostrar
                    viewModel.dismissConnectionStatus()
                }
            }
        }
    }

    private fun updateUi(uiState: WifiScanUiState) {
        // Lógica para mostrar/esconder layouts de estado (Wi-Fi off, Localização off, Conteúdo principal)
        val isLocationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val locationManager = getSystemService(LocationManager::class.java)
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } else {
            true // Localização não é um requisito estrito em APIs mais antigas para scan de Wi-Fi
        }

        if (!uiState.isWifiEnabled) {
            binding.layoutWifiOff.visibility = View.VISIBLE
            binding.layoutLocationOff.visibility = View.GONE
            binding.layoutMainContent.visibility = View.GONE
        } else if (!isLocationEnabled) {
            binding.layoutWifiOff.visibility = View.GONE
            binding.layoutLocationOff.visibility = View.VISIBLE
            binding.layoutMainContent.visibility = View.GONE
        } else {
            binding.layoutWifiOff.visibility = View.GONE
            binding.layoutLocationOff.visibility = View.GONE
            binding.layoutMainContent.visibility = View.VISIBLE

            // Atualiza a RecyclerView
            wifiAdapter.submitList(uiState.availableNetworks)
            wifiAdapter.setSelectedNetwork(uiState.selectedNetwork)

            // Atualiza a rede selecionada
            binding.tvSelectedNetwork.text = "Rede Selecionada: ${uiState.selectedNetwork?.ssid ?: "Nenhuma"}"
            binding.tilPassword.visibility = if (uiState.selectedNetwork != null) View.VISIBLE else View.GONE
            binding.etPassword.setText(uiState.password)
            binding.btnConnect.visibility = if (uiState.selectedNetwork != null) View.VISIBLE else View.GONE
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        )

        val allPermissionsGranted = permissionsToRequest.all {
            checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }

        if (!allPermissionsGranted) {
            requestPermissionLauncher.launch(permissionsToRequest)
        } else {
            viewModel.scanNetworks()
        }
    }
}