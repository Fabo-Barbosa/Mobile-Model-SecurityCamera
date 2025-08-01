package br.com.fabolearn.ezvizshowcamera.presentation.cameralist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.fabolearn.ezvizshowcamera.R
import br.com.fabolearn.ezvizshowcamera.data.model.Camera

class CameraAdapter(
    private val onMenuOptionSelected: (Camera, MenuItemType) -> Unit
) : ListAdapter<Camera, CameraAdapter.CameraViewHolder>(CameraDiffCallback()){

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CameraViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_camera, parent, false)
        return CameraViewHolder(view, onMenuOptionSelected)
    }

    override fun onBindViewHolder(holder: CameraViewHolder, position: Int) {
        val camera = getItem(position)
        holder.bind(camera)
    }

    class CameraViewHolder(
        itemView: View,
        private val onMenuOptionSelected: (Camera, MenuItemType) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val ivCameraImage: ImageView = itemView.findViewById(R.id.ivCameraImage)
        private val tvCameraName: TextView = itemView.findViewById(R.id.tvCameraName)
        private val btnOptionsMenu: ImageButton = itemView.findViewById(R.id.btnOptionsMenu)

        fun bind(camera: Camera) {
            // ivCameraImage.setImageResource(R.drawable.ic_camera_placeholder) // Se tiver imagem real, carregar aqui
            tvCameraName.text = camera.name

            btnOptionsMenu.setOnClickListener {
                showPopupMenu(it, camera)
            }
        }

        private fun showPopupMenu(view: View, camera: Camera) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_camera_options, popup.menu) // Criaremos este menu
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_add_wifi -> {
                        onMenuOptionSelected(camera, MenuItemType.ADD_WIFI)
                        true
                    }
                    R.id.action_rename -> {
                        onMenuOptionSelected(camera, MenuItemType.RENAME)
                        true
                    }
                    R.id.action_watch_live -> {
                        onMenuOptionSelected(camera, MenuItemType.WATCH_LIVE)
                        true
                    }
                    R.id.action_watch_recordings -> {
                        onMenuOptionSelected(camera, MenuItemType.WATCH_RECORDINGS)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private class CameraDiffCallback : DiffUtil.ItemCallback<Camera>() {
        override fun areItemsTheSame(oldItem: Camera, newItem: Camera): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Camera, newItem: Camera): Boolean {
            return oldItem == newItem
        }
    }
}

// Enum para identificar as opções do menu
enum class MenuItemType {
    ADD_WIFI, RENAME, WATCH_LIVE, WATCH_RECORDINGS
}