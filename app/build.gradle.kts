plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

import java.util.Properties

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun signingProperty(name: String): String? {
    keystoreProperties.getProperty(name)?.takeIf { it.isNotBlank() }?.let { return it }
    return when (name) {
        "storeFile" -> System.getenv("KEYSTORE_FILE")
        "storePassword" -> System.getenv("KEYSTORE_PASSWORD")
        "keyAlias" -> System.getenv("KEY_ALIAS")
        "keyPassword" -> System.getenv("KEY_PASSWORD")
        else -> System.getenv(name)
    }?.takeIf { it.isNotBlank() }
}

android {
    namespace = "com.medipro.manager"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.medipro.manager"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 34
        versionName = "1.1.34"
        buildConfigField("String", "LICENSE_API_BASE_URL", "\"https://us-central1-creditmanager-ed58d.cloudfunctions.net\"")
        buildConfigField("boolean", "USE_DEV_LICENSING", "true")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = signingProperty("storeFile") ?: "upload-keystore.jks"
            val keystoreFile = rootProject.file(storeFilePath)
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = signingProperty("storePassword")
                keyAlias = signingProperty("keyAlias")
                keyPassword = signingProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile?.exists() == true &&
                !releaseSigning.storePassword.isNullOrBlank() &&
                !releaseSigning.keyAlias.isNullOrBlank() &&
                !releaseSigning.keyPassword.isNullOrBlank()
            ) {
                signingConfig = releaseSigning
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "USE_DEV_LICENSING", "false")
        }
        debug {
            buildConfigField("boolean", "USE_DEV_LICENSING", "true")
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:security"))
    implementation(project(":core:datastore"))
    implementation(project(":core:worker"))
    implementation(project(":core:database"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:medicine"))
    implementation(project(":feature:license"))
    implementation(project(":feature:supplier"))
    implementation(project(":feature:customer"))
    implementation(project(":feature:purchase"))
    implementation(project(":feature:sales"))
    implementation(project(":feature:inventory"))
    implementation(project(":feature:expiry"))
    implementation(project(":feature:reports"))
    implementation(project(":feature:accounting"))
    implementation(project(":feature:backup"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:notification"))
    implementation(project(":feature:scanner"))
    implementation(project(":feature:globalsearch"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.timber)
    implementation(libs.coil.compose)
    implementation(libs.mlkit.barcode)
    implementation(libs.mlkit.text.recognition)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.crashlytics)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
