# Expo Brownfield — Guia completo

Este projeto usa [`expo-brownfield`](https://docs.expo.dev/versions/latest/sdk/brownfield) na **abordagem isolada**: o módulo React Native / Expo é empacotado como biblioteca nativa (AAR + dependências Maven) e consumido por qualquer app Android existente, **sem necessidade de Node.js ou Metro em produção**.

Documentação oficial:

- [Visão geral brownfield](https://docs.expo.dev/brownfield/overview)
- [Abordagem isolada](https://docs.expo.dev/brownfield/isolated-approach)
- [Referência `expo-brownfield`](https://docs.expo.dev/versions/latest/sdk/brownfield)

---

# Parte 1 — Projeto React / Expo (geração de artefatos)

Esta parte é voltada para quem mantém o código JS/TS e precisa **gerar os artefatos** que serão entregues ao time nativo.

## Pré-requisitos

| Requisito | Detalhe |
|-----------|---------|
| **Node.js** | LTS (este projeto usa v22) |
| **JDK** | 17+ |
| **Android SDK** | `compileSdk 36`, NDK conforme `gradle.properties` |
| **Expo CLI** | Instalado via `npx` (vem com o pacote `expo`) |
| **hermes-compiler** | Declarado em `devDependencies` — versão **deve coincidir** com o `hermes-android` do React Native |

## Estrutura relevante

```
vpm-expo/
├── app/                      # Código JS/TS (expo-router)
├── app.json                  # Configuração Expo + plugin expo-brownfield
├── package.json              # Dependências + scripts + overrides
├── android/                  # Gerado por `expo prebuild` (não commitado)
├── brownfield-output/        # Artefatos Maven gerados (não commitado)
└── brownfield-host-test/     # App Android nativo de teste
```

## Configuração crítica no `package.json`

### Override do `hermes-compiler`

O `react-native@0.83.6` empacota internamente `hermes-compiler@0.14.1` (bytecode v96), mas o runtime `hermes-android` espera **bytecode v98**. O campo `overrides` força todas as resoluções para a versão correta:

```json
{
  "devDependencies": {
    "hermes-compiler": "^250829098.0.2"
  },
  "overrides": {
    "hermes-compiler": "$hermes-compiler"
  }
}
```

> **Sem isso, o app crasha em runtime com `Wrong bytecode version. Expected 98 but got 96`.**

### Cuidados com `react-native-reanimated` / `react-native-worklets`

O `react-native-worklets` usa `eval()` internamente para compilar funções worklet em runtime. O Hermes em builds **release** bloqueia `eval()` (source parsing desabilitado), causando:

```
SyntaxError: Parsing source code unsupported: (function callGuardDEV_reactNati...
```

**Solução:** se o módulo não usa animações Reanimated, **não importe** `react-native-reanimated` nos arquivos de rota/layout:

```tsx
// ❌ NÃO faça isso se não usar animações
import 'react-native-reanimated';

// ✅ Remova o import — expo-router funciona sem ele para navegação básica
```

Se animações forem necessárias no futuro, será preciso avaliar uma versão do `react-native-worklets` que não dependa de `eval()` ou habilitar eval no Hermes.

## Configuração no `app.json`

O plugin `expo-brownfield` define a identidade Maven do artefato:

```json
[
  "expo-brownfield",
  {
    "android": {
      "group": "dev.vpm.vpmexpo",
      "libraryName": "brownfield",
      "package": "dev.vpm.vpmexpo.brownfield",
      "version": "1.0.0",
      "publishing": [
        {
          "type": "localDirectory",
          "name": "dist",
          "path": "brownfield-output"
        }
      ]
    }
  }
]
```

| Campo | Valor atual | Significado |
|-------|-------------|-------------|
| `group` | `dev.vpm.vpmexpo` | groupId Maven |
| `libraryName` | `brownfield` | artifactId Maven |
| `version` | `1.0.0` | Versão do artefato |
| `publishing.path` | `brownfield-output` | Pasta de saída (relativa à raiz) |

A coordenada Maven final é **`dev.vpm.vpmexpo:brownfield:1.0.0`**.

## Gerar artefatos Android

Execute sempre na **raiz do projeto** (onde está `package.json`):

```bash
# 1. Instalar dependências (uma vez, ou após alterar package.json)
npm install

# 2. Gerar/atualizar a pasta android/ com config brownfield
npm run brownfield:prebuild:clean

# 3. Gerar artefatos Maven em brownfield-output/
npm run brownfield:android
```

> **Importante:** o Gradle precisa de `node` no `PATH`. Se usar NVM, garanta que o shell onde roda o comando tem o Node ativo.

Ao final, a pasta `brownfield-output/` conterá a árvore Maven completa:

```
brownfield-output/
├── dev/vpm/vpmexpo/brownfield/1.0.0/   # AAR principal + POM
├── expo/modules/...                     # Módulos Expo transitivos
├── host/exp/exponent/...                # Expo core
└── vpm-expo/com.swmansion.*/...         # Libs nativas (screens, gesture handler, etc.)
```

### Quando regenerar

| Situação | Comando |
|----------|---------|
| Mudou código JS/TS | `npm run brownfield:android` |
| Mudou plugin, `app.json` ou versões nativas | `npm run brownfield:prebuild:clean` → `npm run brownfield:android` |
| Build inconsistente | Deletar `android/` e repetir tudo |

### Scripts npm disponíveis

| Script | Comando real |
|--------|-------------|
| `brownfield:prebuild` | `expo prebuild --platform android` |
| `brownfield:prebuild:clean` | `expo prebuild --platform android --clean` |
| `brownfield:android` | `expo-brownfield build:android --release --repository Dist` |
| `brownfield:android:tasks` | `expo-brownfield tasks:android` |

> Use sempre **`Dist`** com **D maiúsculo** em `--repository`.

## Gerar artefatos iOS

```bash
npm run brownfield:prebuild:ios
npm run brownfield:ios
```

Os artefatos ficam em `brownfield-output/ios/` (XCFramework).

## O que entregar ao integrador

| Plataforma | Entregável | Coordenada |
|------------|-----------|------------|
| **Android** | Zip da pasta **`brownfield-output/`** inteira | `dev.vpm.vpmexpo:brownfield:1.0.0` |
| **iOS** | Zip de **`brownfield-output/ios/`** | — |

Junto com os artefatos, informar:

- Versões: **Expo SDK 55**, **React Native 0.83.6**, **Hermes 250829098.0.2**
- `compileSdk` / `minSdk` usados: **36 / 24**
- Este documento como referência de integração

---

# Parte 2 — Integração no app Android nativo (para integradores)

Esta parte é voltada para o time que mantém o app Android nativo e vai **consumir** os artefatos brownfield.

## Visão geral

O artefato brownfield é uma **biblioteca Android** (AAR) publicada num repositório Maven local (pasta em disco). O AAR contém:

- O **bundle JavaScript** pré-compilado (Hermes bytecode) — não precisa de servidor Node/Metro
- Bibliotecas nativas (`.so`) para todas as ABIs (arm64-v8a, armeabi-v7a, x86, x86_64)
- Dependências transitivas do React Native, Expo e bibliotecas JS

O app host precisa de **4 itens**: repositório Maven, dependência Gradle, `Application` customizado e `Activity` de entrada.

## Passo 1 — Repositório Maven local

Descompacte a pasta `brownfield-output/` recebida em algum lugar acessível pelo projeto. No `settings.gradle.kts`, registre-a como repositório Maven:

```kotlin
// settings.gradle.kts
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven {
            name = "brownfieldLocal"
            url = uri("/CAMINHO/ABSOLUTO/para/brownfield-output")
            // Ou caminho relativo: uri(rootDir.resolve("../brownfield-output").normalize())
        }
        google()
        mavenCentral()
    }
}
```

> A pasta contém vários `groupId`s (não só o AAR principal). Por isso **entregue a pasta inteira**, não só um `.aar`.

## Passo 2 — Dependência Gradle

No `build.gradle.kts` do módulo `app`:

```kotlin
// build.gradle.kts (:app)
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        // ...
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Resolve conflitos de .so duplicadas entre AARs transitivos
    packaging {
        jniLibs {
            pickFirsts += "**/libworklets.so"
            pickFirsts += "**/libc++_shared.so"
        }
    }
}

dependencies {
    implementation("dev.vpm.vpmexpo:brownfield:1.0.0")

    // Dependências mínimas do host (ajustar conforme necessidade)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
```

### Forçar AARs release em builds debug (recomendado)

O AAR brownfield foi gerado com variante **release**. Se o app host compilar em `debug`, o Gradle pode tentar puxar variantes `debug` de `react-android` / `hermes-android` do Maven Central, causando incompatibilidades. Para alinhar:

```kotlin
// build.gradle.kts (:app) — após o bloco android { }
import com.android.build.api.attributes.BuildTypeAttr

afterEvaluate {
    configurations.named("debugRuntimeClasspath").configure {
        attributes {
            attribute(
                BuildTypeAttr.ATTRIBUTE,
                objects.named(BuildTypeAttr::class.java, "release"),
            )
        }
    }
}
```

## Passo 3 — `Application` customizado

Crie (ou modifique) a subclasse de `Application` do app. São necessários **dois** passos na inicialização:

1. **`SoLoader.init`** em `attachBaseContext` — carrega as bibliotecas nativas (Hermes, React Native)
2. **`BrownfieldLifecycleDispatcher.onApplicationCreate`** em `onCreate` — inicializa o runtime Expo

```kotlin
package com.exemplo.meuapp

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.facebook.react.soloader.OpenSourceMergedSoMapping
import com.facebook.soloader.SoLoader
import dev.vpm.vpmexpo.brownfield.BrownfieldLifecycleDispatcher
import java.io.IOException

class MeuApplication : Application() {

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
```

> **Não** chame `ReactNativeApplicationEntryPoint.loadReactNative()` no `Application`. O brownfield já o chama internamente em `ReactNativeHostManager.initialize()`. Chamar os dois gera `Feature flags cannot be overridden more than once`.

## Passo 4 — Activity de entrada para o React Native

Crie uma `Activity` que estende `BrownfieldActivity` e chama `showReactNativeFragment()`:

```kotlin
package com.exemplo.meuapp

import android.os.Bundle
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler
import dev.vpm.vpmexpo.brownfield.BrownfieldActivity
import dev.vpm.vpmexpo.brownfield.showReactNativeFragment

class MinhaExpoActivity : BrownfieldActivity(), DefaultHardwareBackBtnHandler {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showReactNativeFragment()
    }

    override fun invokeDefaultOnBackPressed() {
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
}
```

> O React Native 0.83+ exige que a Activity host implemente `DefaultHardwareBackBtnHandler`. Sem essa interface, ocorre `ClassCastException` em `ReactDelegate.onHostResume`.

## Passo 5 — `AndroidManifest.xml`

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".MeuApplication"
        android:extractNativeLibs="true"
        tools:replace="android:extractNativeLibs"
        ...>

        <!-- Sua activity principal (nativa) -->
        <activity android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Activity que hospeda o módulo React Native -->
        <activity
            android:name=".MinhaExpoActivity"
            android:exported="false"
            android:configChanges="keyboard|keyboardHidden|orientation|screenLayout|screenSize|smallestScreenSize|uiMode"
            android:theme="@style/Theme.AppCompat.NoActionBar" />

    </application>
</manifest>
```

Pontos importantes:

- **`android:extractNativeLibs="true"`** + **`tools:replace`** — garante que as `.so` são extraídas do APK, evitando falhas do SoLoader
- **`android:configChanges`** na Activity React Native — evita que o sistema destrua e recrie a Activity em mudanças de configuração (teclado, rotação, etc.)
- **Tema sem ActionBar** na Activity React Native — o React Native gerencia sua própria UI

## Passo 6 — Abrir a tela React Native

De qualquer lugar do app nativo, inicie a Activity:

```kotlin
// Exemplo: botão na MainActivity nativa
button.setOnClickListener {
    startActivity(Intent(this, MinhaExpoActivity::class.java))
}
```

O bundle JavaScript já está embutido no AAR. **Não precisa** de servidor Node, Metro, rede ou qualquer dependência externa.

---

## Checklist do integrador

- [ ] Pasta `brownfield-output/` descompactada e acessível
- [ ] Repositório Maven local registrado no `settings.gradle.kts`
- [ ] `implementation("dev.vpm.vpmexpo:brownfield:1.0.0")` adicionado
- [ ] `packaging { jniLibs { pickFirsts += ... } }` configurado
- [ ] `afterEvaluate` para forçar AARs release no classpath debug
- [ ] `Application` customizado com `SoLoader.init` + `BrownfieldLifecycleDispatcher`
- [ ] Activity que estende `BrownfieldActivity` + implementa `DefaultHardwareBackBtnHandler`
- [ ] `android:extractNativeLibs="true"` + `tools:replace` no manifest
- [ ] `configChanges` na Activity React Native
- [ ] Tema sem ActionBar na Activity React Native
- [ ] `compileSdk = 36`, `minSdk = 24`, `jvmTarget = "17"`

---

## App de teste incluído (`brownfield-host-test/`)

O repositório inclui um app Android mínimo em `brownfield-host-test/` que implementa todos os passos acima. Use como referência.

### Testar com artefatos embutidos (release, sem Metro)

```bash
# Na raiz vpm-expo — gerar artefatos
npm install
npm run brownfield:prebuild:clean
npm run brownfield:android

# Instalar o app host no device/emulador
npm run brownfield:host:installRelease
```

Abra **"Brownfield host (teste)"** → **"Abrir tela Expo (RN)"**.

### Testar com hot reload (debug, com Metro)

```bash
# Terminal 1 — Metro
npx expo start

# Terminal 2 — instalar e conectar
adb reverse tcp:8081 tcp:8081
npm run brownfield:host:installDebug
```

> O modo debug é para desenvolvimento. Para demonstração ao cliente, sempre use **release**.

---

## Solução de problemas

### `expo-brownfield` falha sem mostrar o erro do Gradle

O CLI só informa que a task falhou. Para ver o motivo real:

```bash
cd android
./gradlew publishBrownfieldReleasePublicationToDistRepository --stacktrace
```

### Erro `BUG! exception in phase 'semantic analysis'`

`android/settings.gradle` corrompido por `expo prebuild` duplicado. Regenerar limpo:

```bash
npm run brownfield:prebuild:clean
npm run brownfield:android
```

### Node não encontrado pelo Gradle

Garanta que `node` está no `PATH` do shell que roda o Gradle. Com NVM:

```bash
export PATH="$HOME/.nvm/versions/node/$(node -v)/bin:$PATH"
```

### `Wrong bytecode version. Expected 98 but got 96`

Falta o `overrides` no `package.json`. Ver seção "Override do hermes-compiler" acima.

### `SoLoaderDSONotFoundError: libhermestooling.so`

O `Application` do host precisa chamar `SoLoader.init(this, OpenSourceMergedSoMapping)` em `attachBaseContext`, **antes** de qualquer outro código React Native. Ver Passo 3.

### `Feature flags cannot be overridden more than once`

`loadReactNative()` só pode rodar **uma vez** por processo. O brownfield já o chama internamente. **Não** chame `ReactNativeApplicationEntryPoint.loadReactNative()` no `Application`.

### `SyntaxError: Parsing source code unsupported`

Causado por bibliotecas que usam `eval()` em runtime (ex.: `react-native-worklets` / `react-native-reanimated`). Hermes bloqueia `eval()` em builds release. Remova imports desnecessários de `react-native-reanimated` ou substitua por alternativas que não usem `eval()`.

---

## Versões utilizadas

| Componente | Versão |
|-----------|--------|
| Expo SDK | 55 |
| React Native | 0.83.6 |
| Hermes (runtime + compiler) | 250829098.0.2 |
| Kotlin | 2.1.20 |
| AGP (Android Gradle Plugin) | 8.12.0 |
| compileSdk | 36 |
| minSdk | 24 |
