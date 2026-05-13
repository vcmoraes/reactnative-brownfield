package dev.vpm.vpmexpo.hosttest

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.facebook.react.soloader.OpenSourceMergedSoMapping
import com.facebook.soloader.SoLoader
import dev.vpm.vpmexpo.brownfield.BrownfieldLifecycleDispatcher
import java.io.IOException

/**
 * O dispatcher Expo pode disparar trabalho JNI antes da activity brownfield rodar
 * `ReactNativeHostManager.initialize()` (onde entra `loadReactNative()`).
 *
 * Só precisamos do **`SoLoader.init(context, OpenSourceMergedSoMapping)`** aqui — igual ao primeiro
 * passo de `ReactNativeApplicationEntryPoint.loadReactNative`. **Não** chame `loadReactNative()` no
 * `Application`: ele também executa `DefaultNewArchitectureEntryPoint.load()`, que só pode rodar **uma
 * vez** por processo; o brownfield chama `loadReactNative()` de novo em `initialize()` e geraria
 * "Feature flags cannot be overridden more than once".
 */
class HostApplication : Application() {
  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(base)
    try {
      SoLoader.init(this, OpenSourceMergedSoMapping)
    } catch (e: IOException) {
      throw RuntimeException(e)
    }
  }

  override fun onCreate() {
    super.onCreate()
    BrownfieldLifecycleDispatcher.onApplicationCreate(this)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    BrownfieldLifecycleDispatcher.onConfigurationChanged(this, newConfig)
  }
}
