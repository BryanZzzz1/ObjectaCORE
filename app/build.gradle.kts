import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // 1. CORRECCIÓN CRÍTICA: Eliminamos el 'version "1.9.22"'.
    // Al dejarlo así, Gradle tomará automáticamente la versión del classpath sin chocar.
    kotlin("plugin.serialization")

    // Mantenemos KSP para que Room pueda compilar su código interno
    alias(libs.plugins.ksp)
}

// 1. CARGA DE CREDENCIALES
val localProperties = Properties()
val propertiesFile = rootProject.file("local.properties")
if (propertiesFile.exists()) {
    propertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}

android {
    namespace = "com.project.objectacore"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.project.objectacore"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 2. INYECCIÓN SEGURA DE VARIABLES
        val supabaseUrl = localProperties.getProperty("SUPABASE_URL") ?: ""
        val supabaseKey = localProperties.getProperty("SUPABASE_KEY") ?: ""
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            pickFirsts.add("**/libc++_shared.so")
        }
    }

}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // --- LIBRERÍAS TÁCTICAS OBJECTA CORE ---

    // Navegación e Íconos
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material-icons-extended")

    // Motor Óptico (CameraX + ML Kit)
    val cameraxVersion = "1.3.3"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // Motor TensorFlow Lite para YOLO (Detección Espacial)
    implementation("com.quickbirdstudios:opencv:4.5.3.0")
    implementation("com.google.android.gms:play-services-tflite-java:16.1.0")
    implementation("com.github.equationl.paddleocr4android:paddleocr4android:v1.2.9")
    implementation("io.coil-kt:coil-svg:2.6.0")

    // Nube (Supabase + Ktor + Serialización)
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.4.0")
    implementation("io.github.jan-tennert.supabase:functions-kt:2.4.0")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.ktor:ktor-client-android:2.3.11")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // --- MOTOR LOCAL (SQLite / Room) ---
    val room_version = "2.8.4"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    // 2. CORRECCIÓN CRÍTICA: Cambiamos annotationProcessor por ksp
    ksp("androidx.room:room-compiler:$room_version")

    // --- SECRETARIADO DE CONFIGURACIÓN (DataStore) ---
    implementation("androidx.datastore:datastore-preferences:1.0.0")
}