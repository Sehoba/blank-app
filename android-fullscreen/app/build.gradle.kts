plugins {
    id("com.android.application")
}

android {
    namespace = "de.moebelschroeder.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "de.moebelschroeder.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.1.0-fullscreen"
    }
}
