plugins {
    id("com.android.application")
}

android {
    namespace = "de.sehoba.selfappbuilder"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.sehoba.selfappbuilder.v2"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
    }
}
