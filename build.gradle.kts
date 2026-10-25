plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.vannstore.admin"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.vannstore.admin"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
dependencies {
    implementation("com.google.android.gms:play-services-location:21.3.0")
}
