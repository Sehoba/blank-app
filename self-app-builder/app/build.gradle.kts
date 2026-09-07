plugins {
    id("com.android.application")
}

android {
    namespace = "de.sehoba.selfappbuilder"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.sehoba.selfappbuilder"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
