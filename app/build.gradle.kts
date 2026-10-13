plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

val portalBaseUrl = providers.gradleProperty("PORTAL_BASE_URL")
    .orElse(providers.environmentVariable("PORTAL_BASE_URL"))
    .getOrElse("http://puntano.gob.ar:8080/portalServer/")
val healthBaseUrl = providers.gradleProperty("SALUD_BASE_URL")
    .orElse(providers.environmentVariable("SALUD_BASE_URL"))
    .getOrElse("http://www.registrosmedicos.sanluis.gob.ar:8080/saludServer/")
val recipeBaseUrl = providers.gradleProperty("RECIPE_BASE_URL")
    .orElse(providers.environmentVariable("RECIPE_BASE_URL"))
    .getOrElse("https://recetadigital.sanluis.gob.ar:8080/recipeServer/")
val educationBaseUrl = providers.gradleProperty("EDU_BASE_URL")
    .orElse(providers.environmentVariable("EDU_BASE_URL"))
    .getOrElse("http://www.Escuelas3punto0.sanluis.gob.ar:8080/educacionalcenterServer/")

android {
    namespace = "com.gpsl.st.portalpuntano5"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.gpsl.st.portalpuntano5"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "5.0.0"
        buildConfigField("String", "PORTAL_BASE_URL", "\"${portalBaseUrl}\"")
        buildConfigField("String", "SALUD_BASE_URL", "\"${healthBaseUrl}\"")
        buildConfigField("String", "RECIPE_BASE_URL", "\"${recipeBaseUrl}\"")
        buildConfigField("String", "EDU_BASE_URL", "\"${educationBaseUrl}\"")
        buildConfigField("boolean", "DEMO_MODE", "false")
    }
    buildTypes { release { isMinifyEnabled = true; isShrinkResources = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
    buildFeatures { buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kxs)
    implementation(libs.okhttp)
    debugImplementation(libs.okhttp.logging)
    implementation(libs.play.code.scanner)
}
