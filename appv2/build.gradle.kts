import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Lee la URL y el token del Web App de integración desde local.properties, que NUNCA se
// sube al repositorio (ver .gitignore) — así el secreto no queda expuesto en GitHub.
// Si el archivo no existe (por ejemplo, en la compilación automática de GitHub Actions),
// ambos quedan vacíos y la app usa datos de ejemplo (MockPassengerRepository) en su lugar.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.incarail.checkinbimodal"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.incarail.checkinbimodal"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-prototipo"

        buildConfigField(
            "String",
            "SALES_API_BASE_URL",
            "\"${localProperties.getProperty("salesApiBaseUrl", "")}\"",
        )
        buildConfigField(
            "String",
            "SALES_API_TOKEN",
            "\"${localProperties.getProperty("salesApiToken", "")}\"",
        )
    }

    buildTypes {
        release {
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

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // Iconos extendidos (Groups, QrCodeScanner, etc.) — el set "core" que trae material3 por
    // defecto no incluye estos, se necesitan explícitamente para los nuevos íconos de esta pantalla.
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // Persistencia local (cache de la lista + cola de sincronización offline)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    // ksp/kapt para Room se agrega al conectar la integración real (ver README)

    // Escaneo de QR (boletos y personal): CameraX para mostrar el visor de cámara en Compose
    // + ML Kit Barcode Scanning para decodificar el QR en el dispositivo (sin llamadas a la
    // nube, sin costo). Ver ui/scan/QrScanner.kt.
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
