package dev.vpm.vpmexpo.hosttest

import android.os.Bundle
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler
import dev.vpm.vpmexpo.brownfield.BrownfieldActivity
import dev.vpm.vpmexpo.brownfield.showReactNativeFragment

/**
 * O React Native 0.83+ exige que a Activity host implemente [DefaultHardwareBackBtnHandler]
 * (cf. ReactDelegate.onHostResume); caso contrário ocorre ClassCastException ao retomar a tela.
 */
class ExpoEmbeddedActivity : BrownfieldActivity(), DefaultHardwareBackBtnHandler {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    showReactNativeFragment()
  }

  override fun invokeDefaultOnBackPressed() {
    @Suppress("DEPRECATION")
    super.onBackPressed()
  }
}
