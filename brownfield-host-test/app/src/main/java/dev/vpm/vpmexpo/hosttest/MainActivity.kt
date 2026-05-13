package dev.vpm.vpmexpo.hosttest

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import kotlin.math.max

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContentView(R.layout.activity_main)

    applySafeAreaInsets()
    syncSystemBarIconContrast()

    findViewById<MaterialButton>(R.id.button_open_expo).setOnClickListener {
      startActivity(Intent(this, ExpoEmbeddedActivity::class.java))
    }
  }

  private fun applySafeAreaInsets() {
    val root = findViewById<View>(R.id.main_root)
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
      val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
      val cutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
      val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
      view.setPadding(
          max(bars.left, cutout.left),
          max(bars.top, cutout.top),
          max(bars.right, cutout.right),
          max(max(bars.bottom, cutout.bottom), ime.bottom),
      )
      windowInsets
    }
    ViewCompat.requestApplyInsets(root)
  }

  /** Ícones escuros nas barras em tema claro (e claros no escuro), alinhado ao Material 3. */
  private fun syncSystemBarIconContrast() {
    val night =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
    WindowCompat.getInsetsController(window, window.decorView).apply {
      isAppearanceLightStatusBars = !night
      isAppearanceLightNavigationBars = !night
    }
  }
}
