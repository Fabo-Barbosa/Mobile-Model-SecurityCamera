package br.com.fabolearn.ezvizshowcamera.presentation.wifiscan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.fabolearn.ezvizshowcamera.R
import br.com.fabolearn.ezvizshowcamera.data.model.WifiNetwork
import br.com.fabolearn.ezvizshowcamera.data.repository.WifiRepository

class WifiNetworkAdapter(
    wifiRepository: WifiRepository,
    private val onNetworkClick: (WifiNetwork) -> Unit
) : ListAdapter<WifiNetwork, WifiNetworkAdapter.WifiNetworkViewHolder>(WifiNetworkDiffCallback()) {

    // Armazena a rede selecionada para controlar o estado da UI
    private var selectedNetwork: WifiNetwork? = wifiRepository.currentConnectedWifi.value

    fun setSelectedNetwork(network: WifiNetwork?) {
        val oldSelected = selectedNetwork
        selectedNetwork = network
        if (oldSelected != null) {
            // Notifica a mudança na posição da rede anteriormente selecionada
            val oldPos = currentList.indexOf(oldSelected)
            if (oldPos != -1) notifyItemChanged(oldPos)
        }
        if (selectedNetwork != null) {
            // Notifica a mudança na posição da nova rede selecionada
            val newPos = currentList.indexOf(selectedNetwork)
            if (newPos != -1) notifyItemChanged(newPos)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiNetworkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wifi_network, parent, false)
        return WifiNetworkViewHolder(view)
    }

    override fun onBindViewHolder(holder: WifiNetworkViewHolder, position: Int) {
        val network = getItem(position)
        holder.bind(network, network == selectedNetwork, onNetworkClick)
    }

    class WifiNetworkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSsid: TextView = itemView.findViewById(R.id.tvSsid)
        private val ivSelectedIndicator: ImageView = itemView.findViewById(R.id.ivSelectedIndicator)
        private val itemLayout: LinearLayout = itemView.findViewById(R.id.itemLayout)


        fun bind(network: WifiNetwork, isSelected: Boolean, onNetworkClick: (WifiNetwork) -> Unit) {
            tvSsid.text = network.ssid
            ivSelectedIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE

            // Altera a cor do texto se estiver selecionado para feedback visual
            val textColor = if (isSelected) {
                ContextCompat.getColor(itemView.context, com.google.android.material.R.color.design_default_color_primary) // Ou uma cor personalizada
            } else {
                ContextCompat.getColor(itemView.context, android.R.color.black) // Cor padrão ou do tema
            }
            tvSsid.setTextColor(textColor)


            itemLayout.setOnClickListener {
                onNetworkClick(network)
            }
        }
    }

    // DiffUtil.Callback para otimizar atualizações da RecyclerView
    private class WifiNetworkDiffCallback : DiffUtil.ItemCallback<WifiNetwork>() {
        override fun areItemsTheSame(oldItem: WifiNetwork, newItem: WifiNetwork): Boolean {
            return oldItem.bssid == newItem.bssid // BSSID é um identificador único para redes Wi-Fi
        }

        override fun areContentsTheSame(oldItem: WifiNetwork, newItem: WifiNetwork): Boolean {
            return oldItem == newItem // Compara todos os campos do data class
        }
    }
}