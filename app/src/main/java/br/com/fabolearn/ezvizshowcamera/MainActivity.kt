package br.com.fabolearn.ezvizshowcamera

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import br.com.fabolearn.ezvizshowcamera.presentation.cameralist.CameraListActivity
import com.videogo.main.EzvizWebViewActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val buttonWifiConfig: Button = findViewById(R.id.buttonConfig)
        val buttonActivityEzviz: Button = findViewById(R.id.buttonEzviz)


        buttonWifiConfig.setOnClickListener {
            val cameraActivity = Intent(this, CameraListActivity::class.java)
            startActivity(cameraActivity)
        }

        buttonActivityEzviz.setOnClickListener {
            val ezvizActivity = Intent(this, EzvizWebViewActivity::class.java)
            startActivity(ezvizActivity)
        }
    }
}
