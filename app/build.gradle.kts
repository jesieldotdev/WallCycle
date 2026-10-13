plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.wallcycle.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wallcycle.app"
        minSdk = 26
        targetSdk = 35
        // No GitHub Actions cada build ganha um número maior, então o Android
        // aceita instalar por cima da versão anterior.
        val build = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = build
        versionName = "1.0.$build"
    }

    // Assinatura fixa: o CI recebe a chave pelos Secrets do repositório
    // (veja o README). Sem ela, cada build gerava uma chave de debug nova e o
    // Android recusava a atualização com "App não instalado".
    val keystorePath = System.getenv("WALLCYCLE_KEYSTORE")
    val fixedKey = keystorePath != null && file(keystorePath).exists()
    if (fixedKey) {
        signingConfigs.create("wallcycle") {
            storeFile = file(keystorePath!!)
            storePassword = System.getenv("WALLCYCLE_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("WALLCYCLE_KEY_ALIAS")
            keyPassword = System.getenv("WALLCYCLE_KEYSTORE_PASSWORD")
        }
    }
    val signing = signingConfigs.getByName(if (fixedKey) "wallcycle" else "debug")

    buildTypes {
        debug {
            signingConfig = signing
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signing
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
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("io.coil-kt:coil-compose:2.7.0")
}
