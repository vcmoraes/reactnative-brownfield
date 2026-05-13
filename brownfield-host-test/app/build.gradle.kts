import com.android.build.api.attributes.BuildTypeAttr
import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.vpm.vpmexpo.hosttest"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.vpm.vpmexpo.hosttest"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    // Assinatura POC: mesmo keystore de debug do Android Studio (~/.android/debug.keystore).
    // Se o arquivo não existir, o release fica unsigned até você gerar o keystore (ex.: rodar um app debug uma vez).
    signingConfigs {
        create("poc") {
            val keystore = File(System.getProperty("user.home"), ".android/debug.keystore")
            if (keystore.isFile) {
                storeFile = keystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val poc = signingConfigs.getByName("poc")
            if (poc.storeFile != null) {
                signingConfig = poc
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            // classpath debug do Gradle pede variant "debug" de react-android/hermes-android.
            // O libhermestooling *debug* referencia símbolos do inspector Hermes que o libhermesvm atual não exporta → dlopen falha.
            // As linhas debugImplementation abaixo forçam os AARs *release* (alinham com brownfield embutido).
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        jniLibs {
            pickFirsts += "**/libworklets.so"
            pickFirsts += "**/libc++_shared.so"
        }
    }
}

// O app continua debugável (buildType debug), mas pedimos AARs *release* de libs Android no classpath —
// evita libhermestooling *debug* do react-android (símbolos inspector Hermes incompatíveis com libhermesvm atual).
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

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("dev.vpm.vpmexpo:brownfield:1.0.0")
}
